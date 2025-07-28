package com.jef.sqlite.management;

import android.database.sqlite.SQLiteDatabase;

import com.jef.sqlite.management.exceptions.SQLiteException;
import com.jef.sqlite.management.interfaces.Column;
import com.jef.sqlite.management.interfaces.Join;
import com.jef.sqlite.management.interfaces.Table;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Clase abstracta que proporciona funcionalidad para gestionar tablas SQLite.
 * Maneja la creación de tablas basadas en anotaciones de entidades.
 *
 * @param <T> El tipo de entidad que representa esta tabla
 */
public abstract class SQLiteTable<T> {

    private final SQLiteManagement management;
    private Class<T> entityClass;

    /**
     * Constructor para SQLiteTable.
     * Inicializa la clase de entidad y crea la tabla en la base de datos.
     *
     * @param management El gestor de la base de datos SQLite
     */
    @SuppressWarnings("unchecked")
    public SQLiteTable(SQLiteManagement management) {
        this.management = management;
        setEntityClass();
        createTable();
    }


    /**
     * Establece la clase de entidad a partir del tipo genérico de la subclase.
     * Este metodo utiliza reflexión para determinar el tipo genérico T.
     * 
     * @throws SQLiteException Si no se puede determinar el tipo de la clase de entidad
     */
    private void setEntityClass() {
        Type superClass = getClass().getGenericSuperclass();
        if (superClass instanceof ParameterizedType) {
            Type type = ((ParameterizedType) superClass).getActualTypeArguments()[0];
            entityClass = (Class<T>) type;
        } else
            throw new SQLiteException("Not able to determine entity class type. Please ensure the subclass is parameterized with a type.\n" +
                    "Class: " + this.getClass().getSimpleName());
    }

    /**
     * Obtiene el gestor de la base de datos SQLite asociado a esta tabla.
     * 
     * @return El objeto SQLiteManagement utilizado por esta tabla
     */
    public SQLiteManagement getManagement() {
        return management;
    }

    /**
     * Creates the table in the database based on the entity class annotations.
     * This method analyzes @Table, @Column, and @Join annotations to generate the SQL creation statement.
     * 
     * @throws SQLiteException If the entity class is not properly annotated or if there are errors in the definition
     */
    private void createTable() {
        if (!entityClass.isAnnotationPresent(Table.class) || entityClass.getAnnotation(Table.class) == null)
            throw new SQLiteException("Entity class " + entityClass.getName() + " is not annotated with @Table");

        Table table = entityClass.getAnnotation(Table.class);

        if (table.name() == null || table.name().isEmpty())
            throw new SQLiteException("Entity class " + entityClass.getName() + " has no table name defined");

        // Get all fields with Column or Join annotations
        List<Field> columnFields = Stream.of(entityClass.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Column.class) || field.isAnnotationPresent(Join.class))
                .collect(Collectors.toList());

        if (columnFields.isEmpty())
            throw new SQLiteException("Entity class " + entityClass.getName() + " has no columns defined");

        StringBuilder createTableSQL = new StringBuilder("CREATE TABLE IF NOT EXISTS ");
        createTableSQL.append(table.name()).append(" (\n");

        // Track column names to avoid duplicates
        Set<String> processedColumnNames = new HashSet<>();

        // Group fields by column name to handle duplicates properly
        Map<String, List<Field>> fieldsByColumnName = new HashMap<>();

        // Populate the map
        for (Field field : columnFields) {
            String columnName = null;
            if (field.isAnnotationPresent(Column.class)) {
                columnName = field.getAnnotation(Column.class).name();
            } else if (field.isAnnotationPresent(Join.class)) {
                columnName = field.getAnnotation(Join.class).targetName();
            }

            if (columnName != null) {
                fieldsByColumnName.computeIfAbsent(columnName, k -> new ArrayList<>()).add(field);
            }
        }

        // Process fields and generate column definitions
        List<String> columnDefinitions = new ArrayList<>();

        for (Map.Entry<String, List<Field>> entry : fieldsByColumnName.entrySet()) {
            String columnName = entry.getKey();
            List<Field> fields = entry.getValue();

            // Determine if any field has a unique constraint
            boolean isUnique = fields.stream().anyMatch(field -> 
                (field.isAnnotationPresent(Column.class) && field.getAnnotation(Column.class).unique()) ||
                (field.isAnnotationPresent(Join.class) && field.getAnnotation(Join.class).unique())
            );

            // Prioritize Column over Join for column definition
            Field fieldToUse = fields.stream()
                .filter(field -> field.isAnnotationPresent(Column.class))
                .findFirst()
                .orElseGet(() -> fields.stream()
                    .filter(field -> field.isAnnotationPresent(Join.class))
                    .findFirst()
                    .orElse(null));

            if (fieldToUse != null) {
                String columnDefinition;

                if (fieldToUse.isAnnotationPresent(Column.class)) {
                    columnDefinition = instructionCreateFromColumn(fieldToUse);
                    // Add UNIQUE if needed and not already present
                    if (isUnique && !columnDefinition.contains("UNIQUE") && 
                        !fieldToUse.getAnnotation(Column.class).unique() && 
                        !fieldToUse.getAnnotation(Column.class).primaryKey()) {
                        columnDefinition += " UNIQUE";
                    }
                } else {
                    columnDefinition = instructionCreateFromJoin(fieldToUse);
                    // Add UNIQUE if needed and not already present
                    if (isUnique && !columnDefinition.contains("UNIQUE") && 
                        !fieldToUse.getAnnotation(Join.class).unique()) {
                        columnDefinition += " UNIQUE";
                    }
                }

                columnDefinitions.add(columnDefinition);
                processedColumnNames.add(columnName);
            }
        }

        createTableSQL.append(String.join(", \n", columnDefinitions));

        // Add foreign key constraints
        List<String> foreignKeyConstraints = columnFields.stream()
                .filter(field -> field.isAnnotationPresent(Join.class))
                .map(this::getForeignKeyConstraint)
                .filter(constraint -> !constraint.isEmpty())
                .collect(Collectors.toList());

        if (!foreignKeyConstraints.isEmpty()) {
            createTableSQL.append(",\n");
            createTableSQL.append(String.join(",\n", foreignKeyConstraints));
        }

        createTableSQL.append("\n);");

        // Execute the SQL statement
        SQLiteDatabase db = management.getWritableDatabase();
        try {
            db.execSQL(createTableSQL.toString());
        } catch (Exception e) {
            throw e;
        } finally {
            db.close();
        }
    }


    /**
     * Generates the SQL instruction to create a column from a field annotated with @Column.
     * 
     * @param field The field representing the column
     * @return The SQL instruction to create the column
     * @throws SQLiteException If the field is not properly annotated or if there are errors in the definition
     */
    private String instructionCreateFromColumn(Field field) {
        Column column = field.getAnnotation(Column.class);
        Class<?> type = field.getType();

        if (column == null)
            throw new SQLiteException("Field " + field.getName() + " in class " + entityClass.getName() + " is not annotated with @Column");

        if (column.name() == null || column.name().isEmpty())
            throw new SQLiteException("Field " + field.getName() + " in class " + entityClass.getName() + " has no column name defined");

        StringBuilder instruction = new StringBuilder();
        instruction.append(column.name())
                  .append(" ")
                  .append(getTypeColumn(type));

        // Add constraints
        if (!column.permitNull())
            instruction.append(" NOT NULL");

        if (column.primaryKey())
            instruction.append(" PRIMARY KEY");

        if (column.autoIncrement())
            instruction.append(" AUTOINCREMENT");

        // Check if this column is a source for any unique join relationship
        boolean isUniqueJoinSource = Stream.of(entityClass.getDeclaredFields())
            .filter(f -> f.isAnnotationPresent(Join.class))
            .map(f -> f.getAnnotation(Join.class))
            .anyMatch(join -> join.unique() && join.source().equals(column.name()));

        // Add UNIQUE constraint if needed
        if ((column.unique() || isUniqueJoinSource) && !column.primaryKey())
            instruction.append(" UNIQUE");

        // Add default value if specified
        instruction.append(getDefaultValue(column, type));

        return instruction.toString();
    }

    /**
     * Generates the SQL instruction to create a column from a field annotated with @Join.
     * Also creates the corresponding foreign key constraint.
     * 
     * @param field The field representing the relationship
     * @return The SQL instruction to create the column
     * @throws SQLiteException If the field is not properly annotated or if there are errors in the definition
     */
    private String instructionCreateFromJoin(Field field) {
        Join join = field.getAnnotation(Join.class);

        if (join == null)
            throw new SQLiteException("Field " + field.getName() + " in class " + entityClass.getName() + " is not annotated with @Join");

        if (join.targetName() == null || join.targetName().isEmpty())
            throw new SQLiteException("Field " + field.getName() + " in class " + entityClass.getName() + " has no target name defined");

        if (join.relationShip() == null)
            throw new SQLiteException("Field " + field.getName() + " in class " + entityClass.getName() + " has no relationship class defined");

        if (join.source() == null || join.source().isEmpty())
            throw new SQLiteException("Field " + field.getName() + " in class " + entityClass.getName() + " has no source column defined");

        // Find the source field in the relationship class to determine the correct column type
        Field sourceField = findFieldByColumnName(join.relationShip(), join.source());
        Class<?> columnType = sourceField != null ? sourceField.getType() : field.getType();

        StringBuilder instruction = new StringBuilder();
        instruction.append(join.targetName())
                  .append(" ")
                  .append(getTypeColumn(columnType));

        // Add constraints
        if (!join.permitNull())
            instruction.append(" NOT NULL");

        if (join.unique())
            instruction.append(" UNIQUE");

        // Add default value if specified
        if (join.defaultValue() != null && !join.defaultValue().isEmpty()) {
            instruction.append(" DEFAULT ");
            if (field.getType() == String.class) {
                instruction.append("'").append(join.defaultValue()).append("'");
            } else {
                instruction.append(join.defaultValue());
            }
        }

        return instruction.toString();
    }

    /**
     * Generates the foreign key constraint for a field annotated with @Join.
     * 
     * @param field The field representing the relationship
     * @return The foreign key constraint or an empty string if not applicable
     */
    private String getForeignKeyConstraint(Field field) {
        if (!field.isAnnotationPresent(Join.class)) {
            return "";
        }

        Join join = field.getAnnotation(Join.class);

        // Only add foreign key constraint if the relationship class has a @Table annotation
        if (join.relationShip().isAnnotationPresent(Table.class)) {
            Table targetTable = join.relationShip().getAnnotation(Table.class);
            return String.format("FOREIGN KEY (%s) REFERENCES %s (%s)", 
                join.targetName(), targetTable.name(), join.source());
        }

        return "";
    }


    /**
     * Determines the SQLite column type corresponding to a Java type.
     * 
     * @param type The Java type
     * @return The corresponding SQLite column type
     * @throws SQLiteException If the type is not compatible with SQLite
     */
    private String getTypeColumn(Class<?> type) {
        // For custom entity classes, use INTEGER for foreign keys
        if (type.getPackage() != null && type.getPackage().getName().startsWith("com.jef.sqlite.management")) {
            return "INTEGER";
        }

        String typeName = type.getSimpleName().toLowerCase();

        // Map Java types to SQLite types
        switch (typeName) {
            case "string":
                return "TEXT";

            case "short":
            case "int":
            case "integer":
            case "boolean":
                return "INTEGER";

            case "long":
            case "date":
                return "BIGINT";

            case "double":
            case "float":
                return "REAL";

            case "byte":
            case "byte[]":
            case "blob":
                return "BLOB"; 

            default:
                throw new SQLiteException("Unsupported type: " + type.getName() + " in table " + entityClass.getName());
        }
    }


    /**
     * Generates the DEFAULT clause for a column based on its type and default value.
     * 
     * @param column The column annotation
     * @param type The Java type of the column
     * @return The DEFAULT clause properly formatted for SQL
     * @throws SQLiteException If the default value is not valid for the specified type
     */
    public String getDefaultValue(Column column, Class<?> type) {
        if (column.defaultValue() == null || column.defaultValue().isEmpty())
            return "";

        String defaultValue = column.defaultValue();
        String typeName = type.getSimpleName().toLowerCase();

        // Format the default value based on the column type
        switch (typeName) {
            case "string":
                return String.format(" DEFAULT '%s'", defaultValue);

            case "short":
            case "int":
            case "integer":
            case "long":
                if (!validateNumber(defaultValue))
                    throw new SQLiteException("Invalid default value for column " + column.name() + ": " + defaultValue);
                return " DEFAULT " + defaultValue;

            case "double":
            case "float":
                if (!validateDouble(defaultValue))
                    throw new SQLiteException("Invalid default value for column " + column.name() + ": " + defaultValue);
                return " DEFAULT " + defaultValue;

            case "boolean":
                String value = defaultValue.toLowerCase().trim();
                if (value.equals("true") || value.equals("1"))
                    return " DEFAULT 1";
                else if (value.equals("false") || value.equals("0"))
                    return " DEFAULT 0";
                else
                    throw new SQLiteException("Invalid default value for column " + column.name() + ": " + defaultValue);

            default:
                throw new SQLiteException("Unsupported type for default value: " + type.getName() + " for column " + column.name());
        }
    }


    /**
     * Validates if a string can be converted to an integer number.
     * 
     * @param number The string to validate
     * @return true if the string is a valid integer number, false otherwise
     */
    public boolean validateNumber(String number) {
        try {
            Long.valueOf(number);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Validates if a string can be converted to a decimal number.
     * 
     * @param number The string to validate
     * @return true if the string is a valid decimal number, false otherwise
     */
    public boolean validateDouble(String number) {
        try {
            Double.valueOf(number);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Finds a field in a class by its column name.
     * 
     * @param clazz The class to search in
     * @param columnName The column name to search for
     * @return The field corresponding to the column, or null if not found
     */
    private Field findFieldByColumnName(Class<?> clazz, String columnName) {
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(Column.class)) {
                Column column = field.getAnnotation(Column.class);
                if (column.name().equals(columnName)) {
                    return field;
                }
            }
        }
        return null;
    }













}
