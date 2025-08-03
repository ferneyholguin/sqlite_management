package com.jef.sqlite.management.Query.QueryInvocation;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.jef.sqlite.management.SQLiteManagement;
import com.jef.sqlite.management.exceptions.SQLiteException;
import com.jef.sqlite.management.interfaces.Column;
import com.jef.sqlite.management.interfaces.Join;
import com.jef.sqlite.management.interfaces.Table;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QueryExistsHandler<T> {

    private final Class<T> entityClass;
    private final SQLiteManagement management;
    private final String tableName;
    private final Map<String, String> fieldToColumn;

    public QueryExistsHandler(Class<T> entityClass, SQLiteManagement management) {
        this.entityClass = entityClass;
        this.management = management;

        if (entityClass.isAnnotationPresent(Table.class))
            this.tableName = entityClass.getAnnotation(Table.class).name();
        else
            throw new IllegalArgumentException("Entity class " + entityClass.getName() + " is not annotated with @Table");

        fieldToColumn = new HashMap<>();

        for (Field field : entityClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(Column.class)) {
                String fieldName = Character.toLowerCase(field.getName().charAt(0)) + field.getName().substring(1);
                fieldToColumn.put(fieldName, field.getAnnotation(Column.class).name());
            } else if (field.isAnnotationPresent(Join.class)) {
                String fieldName = Character.toLowerCase(field.getName().charAt(0)) + field.getName().substring(1);
                fieldToColumn.put(fieldName, field.getAnnotation(Join.class).targetName());
            }
        }

    }

    public boolean handleExistsBy(Method method, Object[] args) {
        if (args == null || args.length < 1)
            throw new SQLiteException("No arguments provided for existsBy method");

        if (!method.getName().startsWith("existsBy"))
            throw new SQLiteException("Method name must start with 'existsBy': " + method.getName());

        String whereClause = extractWhereClause(method);

        // Create the SQL query
        String sql = "SELECT COUNT(*) FROM " + tableName + " WHERE " + whereClause;

        // Execute the query
        return executeExistsQuery(sql, args);
    }

    /**
     * Extracts the WHERE clause from a method name.
     * Parses the method name to extract field names and operators (AND, OR).
     *
     * @param method The method to extract the WHERE clause from
     * @return The SQL WHERE clause (without the "WHERE" keyword)
     * @throws SQLiteException If a field in the method name doesn't exist in the entity
     */
    public String extractWhereClause(Method method) {
        String methodName = method.getName();

        int startIndex = "existsBy".length();
        int endIndex = methodName.length();

        String whereString = methodName.substring(startIndex, endIndex);

        //Se separan las palabras por camelCase incluido los And y Or
        String[] whereCamelCase = splitCamelCase(whereString);

        List<String> parts = new ArrayList<>();

        StringBuilder currentWord = new StringBuilder();
        for (String part : whereCamelCase) {
            if (part.equalsIgnoreCase("and") || part.equalsIgnoreCase("or")) {
                if (currentWord.length() > 0)
                    parts.add(currentWord.toString());

                parts.add(part);
                currentWord = new StringBuilder();
            } else
                currentWord.append(part);
        }

        // Add the last word if it's not empty
        if (currentWord.length() > 0)
            parts.add(currentWord.toString());

        StringBuilder whereClause = new StringBuilder();

        for (String part : parts) {
            if (part.equalsIgnoreCase("and") || part.equalsIgnoreCase("or"))
                whereClause.append(" ").append(part.toUpperCase());
            else {
                String fieldName = Character.toLowerCase(part.charAt(0)) + part.substring(1);
                String columnName = fieldToColumn.get(fieldName);

                if (columnName == null)
                    throw new SQLiteException("Field not found: " + fieldName);

                whereClause.append(" ").append(columnName).append(" = ?");
            }
        }

        return whereClause.toString();
    }

    /**
     * Executes a SQL query to check if any entity matches the criteria.
     *
     * @param sql The SQL query to execute
     * @param args The arguments for the query
     * @return true if any entity matches the criteria, false otherwise
     * @throws SQLiteException If there's an error executing the query
     */
    private boolean executeExistsQuery(String sql, Object[] args) {
        SQLiteDatabase db = management.getReadableDatabase();

        try {
            Cursor cursor = db.rawQuery(sql, createArgs(args));
            boolean exists = false;

            if (cursor.moveToFirst()) {
                exists = cursor.getInt(0) > 0;
            }

            cursor.close();
            return exists;
        } catch (Exception ex) {
            throw new SQLiteException("Error executing query: " + ex.getMessage(), ex);
        } finally {
            db.close();
        }
    }


    /**
     * Converts an array of objects to an array of strings for use in SQL queries.
     * Handles various data types and converts them to appropriate string representations.
     *
     * @param args The array of objects to convert
     * @return An array of strings representing the input objects
     * @throws SQLiteException If an unsupported data type is encountered
     */
    public String[] createArgs(Object[] args) {
        if (args == null || args.length == 0)
            return new String[0];

        String[] result = new String[args.length];
        for(int i = 0; i < args.length; i++) {
            String typeName = args[i].getClass().getSimpleName().toLowerCase();

            switch (typeName) {
                case "string":
                case "short":
                case "int":
                case "integer":
                case "long":
                case "double":
                case "float":
                    result[i] = args[i].toString();
                    break;
                case "boolean":
                    boolean value = (boolean) args[i];
                    result[i] = value ? "1" : "0";
                    break;
                case "date":
                    result[i] = String.valueOf(((Date) args[i]).getTime());
                    break;
                default:
                    throw new SQLiteException("Unsupported type for parameter: " + args[i].getClass().getSimpleName());
            }

        }

        return result;
    }


    /**
     * Splits a camelCase string into an array of words.
     * For example, "findByNameAndAge" would be split into ["find", "By", "Name", "And", "Age"].
     *
     * @param input The camelCase string to split
     * @return An array of words
     */
    public String[] splitCamelCase(String input) {
        List<String> words = new ArrayList<>();
        StringBuilder currentWord = new StringBuilder();

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (i > 0 && Character.isUpperCase(c)) {
                words.add(currentWord.toString());
                currentWord = new StringBuilder();
            }

            currentWord.append(c);
        }

        if (currentWord.length() > 0)
            words.add(currentWord.toString());

        return words.toArray(new String[0]);
    }









}
