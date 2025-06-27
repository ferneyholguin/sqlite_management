package com.jef.sqlite.management;


import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.jef.sqlite.management.exceptions.SQLiteException;
import com.jef.sqlite.management.interfaces.Column;
import com.jef.sqlite.management.interfaces.Join;
import com.jef.sqlite.management.interfaces.Table;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class SQLiteTable<T> {

    private final SQLiteManagement management;
    private Class<T> entityClass;

    @SuppressWarnings("unchecked")
    public SQLiteTable(SQLiteManagement management) {
        this.management = management;
        setEntityClass();
        createTable();
    }


    private void setEntityClass() {
        Type superClass = getClass().getGenericSuperclass();
        if (superClass instanceof ParameterizedType) {
            Type type = ((ParameterizedType) superClass).getActualTypeArguments()[0];
            entityClass = (Class<T>) type;
        } else
            throw new IllegalStateException("Not able to determine entity class type. Please ensure the subclass is parameterized with a type.\nClass: " + this.getClass().getSimpleName());
    }

    public SQLiteManagement getManagement() {
        return management;
    }

    private void createTable() {
        if (!entityClass.isAnnotationPresent(Table.class) || entityClass.getAnnotation(Table.class) == null)
            throw new SQLiteException("Entity class " + entityClass.getName() + " is not annotated with @Table");

        Table table = entityClass.getAnnotation(Table.class);

        if (table.name() == null || table.name().isEmpty())
            throw new SQLiteException("Entity class " + entityClass.getName() + " has no table name defined");

        // Only include Column fields for table creation
        // Join fields are handled separately and don't need columns in the table
        List<Field> columnFields = Stream.of(entityClass.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Column.class))
                .collect(Collectors.toList());

        if (columnFields.isEmpty())
            throw new SQLiteException("Entity class " + entityClass.getName() + " has no columns defined");

        StringBuffer createTableSQL = new StringBuffer("CREATE TABLE IF NOT EXISTS ");

        createTableSQL
                .append(table.name())
                .append(" (\n");

        String instructionsCreateColumns = columnFields.stream()
                .map(this::instructionCreateFromColumn)
                .collect(Collectors.joining(", \n"));

        createTableSQL.append(instructionsCreateColumns);

        createTableSQL.append("\n);");

        // Execute the SQL statement to create the table
        SQLiteDatabase db = management.getWritableDatabase();
        try {
            db.execSQL(createTableSQL.toString());
        } finally {
            db.close();
        }
    }


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

        // Add foreign key constraint
        if (join.relationShip().isAnnotationPresent(Table.class)) {
            Table targetTable = join.relationShip().getAnnotation(Table.class);
            instruction.append(", FOREIGN KEY (\"")
                    .append(join.targetName())
                    .append("\") REFERENCES \"")
                    .append(targetTable.name())
                    .append("\" (\"")
                    .append(join.source())
                    .append("\")");
        }

        return instruction.toString();
    }


    private String getTypeColumn(Class<?> type) {
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


    public boolean validateNumber(String number) {
        try {
            Long.valueOf(number);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }


    public boolean validateDouble(String number) {
        try {
            Double.valueOf(number);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

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
