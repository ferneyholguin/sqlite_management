package com.jef.sqlite.management;


import com.jef.sqlite.management.exceptions.SQLiteException;
import com.jef.sqlite.management.interfaces.Column;
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

        List<Field> fields = Stream.of(entityClass.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Column.class))
                .collect(Collectors.toList());

        if (fields.isEmpty())
            throw new SQLiteException("Entity class " + entityClass.getName() + " has no columns defined");

        StringBuffer createTableSQL = new StringBuffer("CREATE TABLE IF NOT EXISTS ");

        createTableSQL
                .append(table.name())
                .append(" (\n");

        String instructionsCreateColumns = fields.stream()
                .map(this::instructionCreateColumn)
                .collect(Collectors.joining(", \n"));

        createTableSQL.append(instructionsCreateColumns);

        createTableSQL.append("\n);");
    }


    private String instructionCreateColumn(Field field) {
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

        instruction.append(getDefaultValue(column, type));

        return instruction.toString();
    }


    private String getTypeColumn(Class<?> type) {
        return switch (type.getSimpleName().toLowerCase()) {
            case "string" -> "TEXT";
            case "short", "int", "integer", "boolean" -> "INTEGER";
            case "long" -> "BIGINT";
            case "double", "float" -> "REAL";
            case "byte", "byte[]", "blob" -> "BLOB";

            default -> throw new SQLiteException("Unsupported type: " + type.getName()
                    + " in table " + entityClass.getName());
        };

    }


    public String getDefaultValue(Column column, Class<?> type) {
        if (column.defaultValue() == null || column.defaultValue().isEmpty())
            return "";

        StringBuilder defaultValue = new StringBuilder(" DEFAULT ");

        switch (type.getSimpleName().toLowerCase()) {
            case "string" -> defaultValue.append("'").append(column.defaultValue()).append("'");

            case "short", "int", "integer", "long" -> {
                if (validateNumber(column.defaultValue()))
                    defaultValue.append(column.defaultValue());
                else
                    throw new SQLiteException("Invalid default value for column " + column.name() + ": " + column.defaultValue());
            }
            case "double", "float" -> {
                if (validateDouble(column.defaultValue()))
                    defaultValue.append(column.defaultValue());
                else
                    throw new SQLiteException("Invalid default value for column " + column.name() + ": " + column.defaultValue());
            }
            case "boolean" -> {
                String value = column.defaultValue().toLowerCase().trim();
                if (value.equals("true") || value.equals("1"))
                    defaultValue.append("1");
                else if (value.equals("false") || value.equals("0"))
                    defaultValue.append("0");
                else
                    throw new SQLiteException("Invalid default value for column " + column.name() + ": " + column.defaultValue());
                }

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













}
