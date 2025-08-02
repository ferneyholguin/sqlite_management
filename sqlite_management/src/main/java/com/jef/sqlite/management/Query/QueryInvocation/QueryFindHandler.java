package com.jef.sqlite.management.Query.QueryInvocation;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.jef.sqlite.management.SQLiteManagement;
import com.jef.sqlite.management.exceptions.SQLiteException;
import com.jef.sqlite.management.interfaces.Column;
import com.jef.sqlite.management.interfaces.Join;
import com.jef.sqlite.management.interfaces.SQLiteQuery;
import com.jef.sqlite.management.interfaces.Table;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Handler class for find operations in the query system.
 * This class contains all methods related to finding and querying entities.
 * Supports dynamic queries with method name parsing to extract query conditions.
 * 
 * @param <T> The entity type being queried
 */
public class QueryFindHandler<T> {

    private final Class<T> entityClass;
    private final SQLiteManagement management;
    private final String tableName;
    private final Map<String, String> fieldToColumn;

    /**
     * Constructor for QueryFindHandler
     * 
     * @param entityClass The entity class being queried
     * @param management The SQLiteManagement instance
     * @throws IllegalArgumentException If the entity class is not annotated with @Table
     */
    public QueryFindHandler(Class<T> entityClass, SQLiteManagement management) {
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

    /**
     * Main method to handle all find operations.
     * This method parses the method name and delegates to the appropriate handler.
     * 
     * @param method The method being invoked
     * @param args The arguments passed to the method
     * @return The result of the query (entity, list of entities, or Optional)
     * @throws UnsupportedOperationException If the method name is not supported
     */
    public Object handleFindMethod(Method method , Object[] args) {
        if (method.isAnnotationPresent(SQLiteQuery.class))
            return executeCustomQuery(method, args);

        String methodName = method.getName();

        if (methodName.startsWith("find"))
            return executeQuery(method, args);

        throw new UnsupportedOperationException("Method not supported: " + methodName);
    }

    /**
     * Executes a SQL query and returns a list of entities.
     *
     * @param sql The SQL query to execute
     * @param args The arguments for the query
     * @return A list of entities that match the query
     * @throws SQLiteException If there's an error executing the query
     */
    public List<T> queryList(String sql, Object[] args) {
        SQLiteDatabase db = management.getReadableDatabase();
        List<T> results = new ArrayList<>();

        try {
            Cursor cursor = db.rawQuery(sql, createArgs(args));
            while (cursor.moveToNext())
                results.add((T) getResultCursor(cursor, entityClass));

            cursor.close();
        } catch (Exception ex) {
            throw new SQLiteException("Error executing query: " + ex.getMessage(), ex);
        } finally {
            db.close();
        }

        return results;
    }

    /**
     * Executes a SQL query and returns a single entity.
     *
     * @param sql The SQL query to execute
     * @param selectionArgs The arguments for the query
     * @return An Optional containing the entity if found, or empty if not found
     * @throws SQLiteException If there's an error executing the query
     */
    public Optional<T> queryItem(String sql, String[] selectionArgs) {
        SQLiteDatabase db = management.getReadableDatabase();

        try {
            Cursor cursor = db.rawQuery(sql, selectionArgs);
            if (cursor.moveToFirst())
                return Optional.of((T) getResultCursor(cursor, entityClass));
        } catch (Exception ex) {
            throw new SQLiteException("Error executing query: " + ex.getMessage(), ex);
        } finally {
            db.close();
        }

        return Optional.empty();
    }


    /**
     * Creates an entity instance from a database cursor.
     *
     * @param cursor The database cursor positioned at the row to read
     * @return An instance of the entity populated with data from the cursor
     * @throws SQLiteException If there's an error creating the entity
     */
    public Object getResultCursor(Cursor cursor, Class<?> entityClass) {
        try {
            Object instance = entityClass.newInstance();

            // Process all fields
            for (Field field : entityClass.getDeclaredFields()) {
                if (field.isAnnotationPresent(Column.class))
                    processColumnField(cursor, instance, field);
                else if (field.isAnnotationPresent(Join.class))
                    processJoinField(cursor, instance, field);

            }

            return instance;
        } catch (Exception e) {
            throw new SQLiteException("Error creating entity from cursor: " + e.getMessage(), e);
        }
    }

    /**
     * Processes a field annotated with @Column, setting its value from the cursor.
     *
     * @param cursor The database cursor
     * @param instance The entity instance
     * @param field The field to process
     * @throws Exception If there's an error processing the field
     */
    private void processColumnField(Cursor cursor, Object instance, Field field) throws Exception {
        Column column = field.getAnnotation(Column.class);
        String columnName = column.name();
        int columnIndex = cursor.getColumnIndex(columnName);

        if (columnIndex == -1)
            return;

        field.setAccessible(true);
        Class<?> fieldType = field.getType();

        if (cursor.isNull(columnIndex)) {
            field.set(instance, null);
            return;
        }

        if (fieldType == String.class)
            field.set(instance, cursor.getString(columnIndex));
        else if (fieldType == short.class || fieldType == Short.class)
            field.set(instance, cursor.getShort(columnIndex));
        else if (fieldType == int.class || fieldType == Integer.class)
            field.set(instance, cursor.getInt(columnIndex));
        else if (fieldType == long.class || fieldType == Long.class)
            field.set(instance, cursor.getLong(columnIndex));
        else if (fieldType == double.class || fieldType == Double.class)
            field.set(instance, cursor.getDouble(columnIndex));
        else if (fieldType == float.class || fieldType == Float.class)
            field.set(instance, cursor.getFloat(columnIndex));
        else if (fieldType == boolean.class || fieldType == Boolean.class)
            field.set(instance, cursor.getInt(columnIndex) == 1);
        else if (fieldType == byte.class || fieldType == Byte.class)
            field.set(instance, cursor.getShort(columnIndex));
        else if (fieldType == byte[].class)
            field.set(instance, cursor.getBlob(columnIndex));
        else if (fieldType == Date.class)
            field.set(instance, new Date(cursor.getLong(columnIndex)));

    }

    /**
     * Processes a field annotated with @Join, loading the related entity.
     *
     * @param instance The entity instance
     * @param field The field to process
     * @throws Exception If there's an error processing the field
     */
    private void processJoinField(Cursor cursor, Object instance, Field field) throws Exception {
        Join join = field.getAnnotation(Join.class);
        Class<?> relationshipClass = join.relationShip();

        if (relationshipClass == null)
            throw new SQLiteException("Relationship class not specified for @Join field: " + field.getName());
        if (relationshipClass.getAnnotation(Table.class) == null)
            throw new SQLiteException("Relationship class is not annotated with @Table: " + relationshipClass.getName());

        Field fieldJoin = Stream.of(relationshipClass.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(Column.class))
                .filter(f -> f.getName().equalsIgnoreCase(join.source()))
                .findFirst()
                .orElse(null);

        String sourceColumnName = fieldJoin.getAnnotation(Column.class).name();

        int columnIndex = cursor.getColumnIndex(join.targetName());

        if (columnIndex == -1)
            return;

        if (cursor.isNull(columnIndex))
            return;

        String targetNameValue = cursor.getString(columnIndex);

        // Create a query to find the related entity
        final String sql = "SELECT * FROM " + relationshipClass.getAnnotation(Table.class).name() +
                " WHERE " + sourceColumnName + " = ?";

        // Execute the query
        SQLiteDatabase db = management.getReadableDatabase();
        try {
            Cursor cursorJoin = db.rawQuery(sql, new String[] { targetNameValue });
            if (cursorJoin.moveToFirst()) {

                Object relatedInstance = getResultCursor(cursorJoin, relationshipClass);

                // Set the related instance in the main entity
                field.setAccessible(true);
                field.set(instance, relatedInstance);
            }

            cursorJoin.close();
        } finally {
            db.close();
        }
    }

    /**
     * Executes a custom SQL query defined in a SQLiteQuery annotation.
     *
     * @param method The method annotated with SQLiteQuery
     * @param args The arguments passed to the method
     * @return The result of the query, either a List<T> or Optional<T> depending on the method's return type
     * @throws SQLiteException If there's an error executing the query
     */
    public Object executeCustomQuery(Method method, Object[] args) {
        // Get the SQL query from the annotation
        SQLiteQuery annotation = method.getAnnotation(SQLiteQuery.class);
        if (annotation == null)
            throw new SQLiteException("Method is not annotated with SQLiteQuery: " + method.getName());

        String sql = annotation.sql();
        if (sql == null || sql.isEmpty())
            throw new SQLiteException("SQL query cannot be empty in SQLiteQuery annotation");

        final String[] queryArgs = createArgs(args);

        // Check the return type and execute the appropriate query
        Class<?> returnType = method.getReturnType();
        if (List.class.isAssignableFrom(returnType)) {
            return queryList(sql, queryArgs);
        } else if (Optional.class.isAssignableFrom(returnType)) {
            return queryItem(sql, queryArgs);
        } else {
            throw new SQLiteException("Unsupported return type for SQLiteQuery: " + returnType.getName() +
                    ". Must be List<T> or Optional<T>");
        }
    }


    /**
     * Executes a query based on the method name.
     * Parses the method name to extract query conditions and order by clauses.
     * 
     * @param method The method being invoked
     * @param args The arguments passed to the method
     * @return The result of the query, either a List<T> or Optional<T> depending on the method's return type
     * @throws SQLiteException If there's an error executing the query or if the return type is not supported
     */
    public Object executeQuery(Method method, Object[] args) {
        String methodName = method.getName();

        String[] arguments = createArgs(args);
        String sql = "SELECT * FROM " + tableName;

        if (methodName.startsWith("findBy")) {
            String whereClause = extractWhereClause(method);
            String orderByClause = extractOrderByClause(method);

            sql += " WHERE " + whereClause;

            if (orderByClause != null && !orderByClause.isEmpty())
                sql += " ORDER BY " + orderByClause;

        } else if (methodName.startsWith("findAll")) {
            String orderByClause = extractOrderByClause(method);

            if (orderByClause != null && !orderByClause.isEmpty())
                sql += " ORDER BY " + orderByClause;
        }

        Class<?> returnType = method.getReturnType();

        if (List.class.isAssignableFrom(returnType)) {
            return queryList(sql, arguments);
        } else if (Optional.class.isAssignableFrom(returnType)) {
            return queryItem(sql, arguments);
        } else {
            throw new SQLiteException("Unsupported return type for method: " + returnType.getName() +
                    ". Must be List<T> or Optional<T>");
        }

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

        int startIndex = "findBy".length();
        int endIndex = methodName.indexOf("OrderBy");

        // Si no hay "OrderBy", usar toda la cadena después de "findBy"
        if (endIndex == -1)
            endIndex = methodName.length();

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
     * Extracts the ORDER BY clause from a method name.
     * Parses the method name to extract field names and sort direction (ASC, DESC).
     * 
     * @param method The method to extract the ORDER BY clause from
     * @return The SQL ORDER BY clause (without the "ORDER BY" keyword), or null if no ORDER BY clause is present
     * @throws SQLiteException If a field in the method name doesn't exist in the entity
     */
    public String extractOrderByClause(Method method) {
        String methodName = method.getName();

        int startIndex = methodName.indexOf("OrderBy");
        int endIndex = methodName.length();

        if (startIndex == -1)
            return null;

        String orderByString =
                methodName.substring(startIndex + "OrderBy".length(), endIndex)
                        .replace("ASC", "Asc")
                        .replace("DESC", "Desc");

        String[] orderByCamelCase = splitCamelCase(orderByString);

        List<String> parts = new ArrayList<>();

        StringBuilder currentWord = new StringBuilder();
        for (String part : orderByCamelCase) {
            if (part.equalsIgnoreCase("asc") || part.equalsIgnoreCase("desc")) {
                if (currentWord.length() > 0)
                    parts.add(currentWord.toString());

                parts.add(part);
                currentWord = new StringBuilder();
            } else
                currentWord.append(part);
        }

        StringBuilder orderByClause = new StringBuilder();

        for (String part : parts) {
            if (part.equalsIgnoreCase("asc") || part.equalsIgnoreCase("desc"))
                orderByClause.append(" ").append(part);
            else {
                String fieldName = Character.toLowerCase(part.charAt(0)) + part.substring(1);
                String columnName = fieldToColumn.get(fieldName);

                if (columnName == null)
                    throw new SQLiteException("Field not found: " + fieldName);

                orderByClause.append(" ").append(columnName);
            }
        }

        return orderByClause.toString();
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
