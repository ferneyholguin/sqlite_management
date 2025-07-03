package com.jef.sqlite.management.Query.QueryInvocation;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.jef.sqlite.management.SQLiteManagement;
import com.jef.sqlite.management.exceptions.SQLiteException;
import com.jef.sqlite.management.interfaces.Column;
import com.jef.sqlite.management.interfaces.Table;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handler class for existence check operations in the query system.
 * This class contains methods to check if entities exist based on various criteria.
 * 
 * @param <T> The entity type being queried
 */
public class QueryExistsHandler<T> {

    private final Class<T> entityClass;
    private final SQLiteManagement management;
    private final String tableName;
    private final Map<String, String> fieldToColumn;

    /**
     * Constructor for QueryExistsHandler
     * 
     * @param entityClass The entity class being queried
     * @param management The SQLiteManagement instance
     */
    public QueryExistsHandler(Class<T> entityClass, SQLiteManagement management) {
        this.entityClass = entityClass;
        this.management = management;
        this.tableName = entityClass.getAnnotation(Table.class).name();
        this.fieldToColumn = new HashMap<>();

        for (Field field : entityClass.getDeclaredFields())
            if (field.isAnnotationPresent(Column.class))
                fieldToColumn.put(field.getName().toLowerCase(), field.getAnnotation(Column.class).name());
    }

    /**
     * Handles method calls for existence checks.
     * Parses the method name to determine the fields and operators to use in the query.
     * 
     * @param method The method being invoked
     * @param args The arguments passed to the method
     * @return true if any entity matches the criteria, false otherwise
     * @throws SQLiteException If there's an error in the query or method name format
     */
    public boolean handleExistsBy(Method method, Object[] args) {
        String methodName = method.getName();

        if (!methodName.startsWith("existsBy"))
            throw new SQLiteException("Method name must start with 'existsBy': " + methodName);

        // Extract the criteria part (after "existsBy")
        String criteria = methodName.substring("existsBy".length());

        // Parse the criteria to build the WHERE clause
        String whereClause = buildWhereClause(criteria);

        // Create the SQL query
        String sql = "SELECT COUNT(*) FROM " + tableName + " WHERE " + whereClause;

        // Execute the query
        return executeExistsQuery(sql, args);
    }

    /**
     * Builds a WHERE clause from the criteria part of the method name.
     * Supports "And" and "Or" operators.
     * 
     * @param criteria The criteria part of the method name (after "existsBy")
     * @return The WHERE clause for the SQL query
     * @throws SQLiteException If a field in the criteria doesn't exist
     */
    private String buildWhereClause(String criteria) {
        StringBuilder whereClause = new StringBuilder();

        // Split the criteria by "And" and "Or" operators
        Pattern pattern = Pattern.compile("(And|Or)");
        String[] parts = pattern.split(criteria);

        // Find all operators in the criteria
        Matcher matcher = pattern.matcher(criteria);
        String[] operators = new String[parts.length - 1];
        int i = 0;
        while (matcher.find()) {
            operators[i++] = matcher.group();
        }

        // Build the WHERE clause
        for (i = 0; i < parts.length; i++) {
            // Convert the field name to camelCase for lookup
            String fieldName = Character.toLowerCase(parts[i].charAt(0)) + parts[i].substring(1);

            // Get the column name
            String columnName = fieldToColumn.get(fieldName.toLowerCase());
            if (columnName == null)
                throw new SQLiteException("Field not found: " + fieldName);

            // Add the field condition
            whereClause.append(columnName).append(" = ?");

            // Add the operator if not the last part
            if (i < operators.length) {
                whereClause.append(" ").append(operators[i]).append(" ");
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
     * Converts method arguments to string array for SQL query parameters.
     * 
     * @param args The method arguments
     * @return String array of argument values
     * @throws SQLiteException If an argument type is not supported
     */
    private String[] createArgs(Object[] args) {
        if (args == null || args.length == 0)
            return new String[0];

        String[] result = new String[args.length];
        for (int i = 0; i < args.length; i++) {
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
                default:
                    throw new SQLiteException("Unsupported type for parameter: " + args[i].getClass().getSimpleName());
            }
        }

        return result;
    }





}
