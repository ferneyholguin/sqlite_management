package com.jef.sqlite.management;

import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;

import com.jef.sqlite.management.exceptions.SQLiteException;
import com.jef.sqlite.management.interfaces.Column;
import com.jef.sqlite.management.interfaces.Join;
import com.jef.sqlite.management.interfaces.Table;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SQLiteTable<T> {

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

    private void createTable() {
        Table table = entityClass.getAnnotation(Table.class);

        if (table == null)
            throw new SQLiteException("Entity class " + entityClass.getName() + " is not annotated with @Table");

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


        boolean isFirstColumn = true;
        for (Field columnField : columnFields) {
            if (columnField.isAnnotationPresent(Column.class)) {
                String createColumn = instructionCreateColumn(columnField);
                if (isFirstColumn) {
                    createTableSQL.append("\n").append(createColumn);
                    isFirstColumn = false;
                } else {
                    createTableSQL.append(",\n").append(createColumn);
                }
            } else if (columnField.isAnnotationPresent(Join.class)) {
                String createColumn = instructionCreateJoin(columnField);
                if (isFirstColumn) {
                    createTableSQL.append("\n").append(createColumn);
                    isFirstColumn = false;
                } else {
                    createTableSQL.append(",\n").append(createColumn);
                }
            }
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

    private String instructionCreateColumn(@NonNull Field field) {
        Column column = field.getAnnotation(Column.class);

        if (column == null || column.name() == null || column.name().isEmpty())
            throw new SQLiteException("Field " + field.getName() + " in class " + entityClass.getName() + " has no column name defined");

        StringBuilder instruction = new StringBuilder();
        instruction.append(column.name())
                .append(" ")
                .append(getTypeColumn(field.getType()));

        // Add constraints
        if (!column.permitNull())
            instruction.append(" NOT NULL");

        if (column.primaryKey())
            instruction.append(" PRIMARY KEY");

        if (column.autoIncrement())
            instruction.append(" AUTOINCREMENT");

        if (column.unique())
            instruction.append(" UNIQUE");

        if (column.defaultValue() != null && !column.defaultValue().isEmpty())
            instruction.append(getDefaultValue(column.defaultValue(), field.getType()));

        return instruction.toString();
    }

    private String instructionCreateJoin(@NonNull Field field) {
        Join join = field.getAnnotation(Join.class);

        if (join == null)
            throw new SQLiteException("Field " + field.getName() + " in class " + entityClass.getName() + " is not annotated with @Join");

        if (join.targetName() == null || join.targetName().isEmpty())
            throw new SQLiteException("Field " + field.getName() + " in class " + entityClass.getName() + " has no target name defined");

        if (join.relationShip() == null)
            throw new SQLiteException("Field " + field.getName() + " in class " + entityClass.getName() + " has no relationship class defined");

        if (join.source() == null || join.source().isEmpty())
            throw new SQLiteException("Field " + field.getName() + " in class " + entityClass.getName() + " has no source column defined");


        Field sourceField = Arrays.stream(join.relationShip().getDeclaredFields())
                .filter(f -> {
                    if (!f.isAnnotationPresent(Column.class))
                        return false;

                    Column column = f.getAnnotation(Column.class);
                    if (column == null)
                        return false;

                    return column.name().equals(join.source());
                })
                .findFirst()
                .orElse(null);

        if (sourceField == null)
            throw new SQLiteException("No found source " + join.source() + " in class " + entityClass.getName() + " of field " + field.getName());

        Class<?> columnType = sourceField.getType();


        StringBuilder instruction = new StringBuilder();
        instruction.append(join.targetName())
                .append(" ")
                .append(getTypeColumn(columnType));

        // Add constraints
        if (!join.permitNull())
            instruction.append(" NOT NULL");

        if (join.unique())
            instruction.append(" UNIQUE");

        if (join.defaultValue() != null && !join.defaultValue().isEmpty())
            instruction
                    .append(getDefaultValue(join.defaultValue(), columnType));

        return instruction.toString();
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

    public String getDefaultValue(String defaultValue, Class<?> type) {
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
                    throw new SQLiteException("Invalid default value for column: " + defaultValue);
                return " DEFAULT " + defaultValue;

            case "double":
            case "float":
                if (!validateDouble(defaultValue))
                    throw new SQLiteException("Invalid default value for column: " + defaultValue);
                return " DEFAULT " + defaultValue;

            case "boolean":
                String value = defaultValue.toLowerCase().trim();
                if (value.equals("true") || value.equals("1"))
                    return " DEFAULT 1";
                else if (value.equals("false") || value.equals("0"))
                    return " DEFAULT 0";
                else
                    throw new SQLiteException("Invalid default value for column: " + defaultValue);


            default:
                throw new SQLiteException("Unsupported type for default value: " + type.getName() + " value: " + defaultValue);
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





}
