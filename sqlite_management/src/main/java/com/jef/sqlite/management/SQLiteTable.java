package com.jef.sqlite.management;


import android.database.sqlite.SQLiteDatabase;

import com.jef.sqlite.management.exceptions.SQLiteException;
import com.jef.sqlite.management.interfaces.Column;
import com.jef.sqlite.management.interfaces.Join;
import com.jef.sqlite.management.interfaces.Table;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Objects;
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
     * Crea la tabla en la base de datos basada en las anotaciones de la clase de entidad.
     * Este metodo analiza las anotaciones @Table, @Column y @Join para generar el SQL de creación.
     * 
     * @throws SQLiteException Si la clase de entidad no está correctamente anotada o si hay errores en la definición
     */
    private void createTable() {
        if (!entityClass.isAnnotationPresent(Table.class) || entityClass.getAnnotation(Table.class) == null)
            throw new SQLiteException("Entity class " + entityClass.getName() + " is not annotated with @Table");

        Table table = entityClass.getAnnotation(Table.class);

        if (table.name() == null || table.name().isEmpty())
            throw new SQLiteException("Entity class " + entityClass.getName() + " has no table name defined");

        // Only include Column fields for table creation
        // Join fields are handled separately and don't need columns in the table
        List<Field> columnFields = Stream.of(entityClass.getDeclaredFields())
                .filter(field ->
                        field.isAnnotationPresent(Column.class) || field.isAnnotationPresent(Join.class))
                .collect(Collectors.toList());

        if (columnFields.isEmpty())
            throw new SQLiteException("Entity class " + entityClass.getName() + " has no columns defined");

        StringBuffer createTableSQL = new StringBuffer("CREATE TABLE IF NOT EXISTS ");

        createTableSQL
                .append(table.name())
                .append(" (\n");

        // Generate column definitions
        String instructionsCreateColumns = columnFields.stream()
                .map(field -> {
                    if (field.isAnnotationPresent(Column.class))
                        return instructionCreateFromColumn(field);
                    else if (field.isAnnotationPresent(Join.class))
                        return instructionCreateFromJoin(field);
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.joining(", \n"));

        createTableSQL.append(instructionsCreateColumns);

        // Collect and add foreign key constraints
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

        // Execute the SQL statement to create the table
        SQLiteDatabase db = management.getWritableDatabase();
        try {
            db.execSQL(createTableSQL.toString());
        } finally {
            db.close();
        }
    }


    /**
     * Genera la instrucción SQL para crear una columna a partir de un campo anotado con @Column.
     * 
     * @param field El campo que representa la columna
     * @return La instrucción SQL para crear la columna
     * @throws SQLiteException Si el campo no está correctamente anotado o si hay errores en la definición
     */
    private String instructionCreateFromColumn(Field field) {
        Column column = field.getAnnotation(Column.class);
        Class<?> type = field.getType();

        if (column == null)
            throw new SQLiteException("Field " + field.getName() + " in class " + entityClass.getName() + " is not annotated with @Column");

        if (column.name() == null || column.name().isEmpty())
            throw new SQLiteException("Field " + field.getName() + " in class " + entityClass.getName() + " has no column name defined");

        StringBuilder instruction = new StringBuilder();

        instruction
                .append("\"")
                .append(column.name())
                .append("\" ")
                .append(getTypeColumn(type));

        if (!column.permitNull())
            instruction.append(" NOT NULL");

        if (column.isPrimaryKey())
            instruction.append(" PRIMARY KEY");

        if (column.isAutoIncrement())
            instruction.append(" AUTOINCREMENT");

        // Add UNIQUE constraint if the column itself is marked as unique
        // or if it's a source for a unique join relationship
        boolean isUniqueJoinSource = false;
        for (Field joinField : entityClass.getDeclaredFields()) {
            if (joinField.isAnnotationPresent(Join.class)) {
                Join join = joinField.getAnnotation(Join.class);
                if (join.isUnique() && join.source().equals(column.name())) {
                    isUniqueJoinSource = true;
                    break;
                }
            }
        }

        if ((column.isUnique() || isUniqueJoinSource) && !column.isPrimaryKey())
            instruction.append(" UNIQUE");

        instruction.append(getDefaultValue(column, type));

        return instruction.toString();
    }

    /**
     * Genera la instrucción SQL para crear una columna a partir de un campo anotado con @Join.
     * También crea la restricción de clave foránea correspondiente.
     * 
     * @param field El campo que representa la relación
     * @return La instrucción SQL para crear la columna y la restricción de clave foránea
     * @throws SQLiteException Si el campo no está correctamente anotado o si hay errores en la definición
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

        StringBuilder instruction = new StringBuilder();

        // Find the source field in the relationship class to determine the correct column type
        Field sourceField = findFieldByColumnName(join.relationShip(), join.source());
        Class<?> columnType = sourceField != null ? sourceField.getType() : field.getType();

        instruction
                .append("\"")
                .append(join.targetName())
                .append("\" ")
                .append(getTypeColumn(columnType));

        if (!join.permitNull())
            instruction.append(" NOT NULL");

        if (join.isUnique())
            instruction.append(" UNIQUE");

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
     * Genera la restricción de clave foránea para un campo anotado con @Join.
     * 
     * @param field El campo que representa la relación
     * @return La restricción de clave foránea o una cadena vacía si no aplica
     */
    private String getForeignKeyConstraint(Field field) {
        if (!field.isAnnotationPresent(Join.class)) {
            return "";
        }

        Join join = field.getAnnotation(Join.class);

        // Add foreign key constraint
        if (join.relationShip().isAnnotationPresent(Table.class)) {
            Table targetTable = join.relationShip().getAnnotation(Table.class);
            return "FOREIGN KEY (\"" + join.targetName() + "\") REFERENCES \"" + 
                   targetTable.name() + "\" (\"" + join.source() + "\")";
        }

        return "";
    }


    /**
     * Determina el tipo de columna SQLite correspondiente a un tipo de Java.
     * 
     * @param type El tipo de Java
     * @return El tipo de columna SQLite correspondiente
     * @throws SQLiteException Si el tipo no es compatible con SQLite
     */
    private String getTypeColumn(Class<?> type) {
        // If the type is a custom class (not a primitive or standard Java type),
        // we'll use INTEGER as the type for the foreign key
        if (type.getPackage() != null && type.getPackage().getName().startsWith("com.jef.sqlite.management")) {
            return "INTEGER";
        }

        String typeName = type.getSimpleName().toLowerCase();
        switch (typeName) {
            case "string":
                return "TEXT";
            case "short":
            case "int":
            case "integer":
            case "boolean":
                return "INTEGER";
            case "long":
                return "BIGINT";
            case "double":
            case "float":
                return "REAL";
            case "byte":
            case "byte[]":
            case "blob":
                return "BLOB";
            default:
                throw new SQLiteException("Unsupported type: " + type.getName()
                        + " in table " + entityClass.getName());
        }
    }


    /**
     * Genera la cláusula DEFAULT para una columna basada en su tipo y valor predeterminado.
     * 
     * @param column La anotación de columna
     * @param type El tipo de Java de la columna
     * @return La cláusula DEFAULT formateada correctamente para SQL
     * @throws SQLiteException Si el valor predeterminado no es válido para el tipo especificado
     */
    public String getDefaultValue(Column column, Class<?> type) {
        if (column.defaultValue() == null || column.defaultValue().isEmpty())
            return "";

        StringBuilder defaultValue = new StringBuilder(" DEFAULT ");
        String typeName = type.getSimpleName().toLowerCase();

        switch (typeName) {
            case "string":
                defaultValue.append("'").append(column.defaultValue()).append("'");
                break;
            case "short":
            case "int":
            case "integer":
            case "long":
                if (validateNumber(column.defaultValue()))
                    defaultValue.append(column.defaultValue());
                else
                    throw new SQLiteException("Invalid default value for column " + column.name() + ": " + column.defaultValue());
                break;
            case "double":
            case "float":
                if (validateDouble(column.defaultValue()))
                    defaultValue.append(column.defaultValue());
                else
                    throw new SQLiteException("Invalid default value for column " + column.name() + ": " + column.defaultValue());
                break;
            case "boolean":
                String value = column.defaultValue().toLowerCase().trim();
                if (value.equals("true") || value.equals("1"))
                    defaultValue.append("1");
                else if (value.equals("false") || value.equals("0"))
                    defaultValue.append("0");
                else
                    throw new SQLiteException("Invalid default value for column " + column.name() + ": " + column.defaultValue());
                break;
            default:
                throw new SQLiteException("Unsupported type for default value: " + type.getName() + " for column " + column.name());
        }

        return defaultValue.toString();
    }


    /**
     * Valida si una cadena puede convertirse a un número entero.
     * 
     * @param number La cadena a validar
     * @return true si la cadena es un número entero válido, false en caso contrario
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
     * Valida si una cadena puede convertirse a un número decimal.
     * 
     * @param number La cadena a validar
     * @return true si la cadena es un número decimal válido, false en caso contrario
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
     * Busca un campo en una clase por el nombre de su columna.
     * 
     * @param clazz La clase en la que buscar
     * @param columnName El nombre de la columna a buscar
     * @return El campo correspondiente a la columna, o null si no se encuentra
     */
    private Field findFieldByColumnName(Class<?> clazz, String columnName) {
        for (Field field : clazz.getDeclaredFields()) 
            if (field.isAnnotationPresent(Column.class)) {
                Column column = field.getAnnotation(Column.class);

                if (column.name().equals(columnName))
                    return field;

            }

        return null;
    }













}
