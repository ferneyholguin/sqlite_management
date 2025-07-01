package com.jef.sqlite.management.Query.QueryInvocation;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.jef.sqlite.management.Query.QueryFactory;
import com.jef.sqlite.management.SQLiteManagement;
import com.jef.sqlite.management.exceptions.SQLiteException;
import com.jef.sqlite.management.interfaces.Column;
import com.jef.sqlite.management.interfaces.DynamicQuery;
import com.jef.sqlite.management.interfaces.Join;
import com.jef.sqlite.management.interfaces.SQLiteQuery;
import com.jef.sqlite.management.interfaces.Table;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Handler class for find operations in the query system.
 * This class contains all methods related to finding and querying entities.
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
     */
    public QueryFindHandler(Class<T> entityClass, SQLiteManagement management) {
        this.entityClass = entityClass;
        this.management = management;
        this.tableName = entityClass.getAnnotation(Table.class).name();
        this.fieldToColumn = new HashMap<>();

        for (Field field : entityClass.getDeclaredFields())
            if (field.isAnnotationPresent(Column.class))
                fieldToColumn.put(field.getName().toLowerCase(), field.getAnnotation(Column.class).name());
    }

    /**
     * Crea una consulta para buscar entidades por un valor específico de un campo.
     * Analiza el nombre del metodo para determinar el campo por el que buscar.
     * 
     * @param method El metodo invocado
     * @param args Los argumentos pasados al metodo (valor a buscar)
     * @return Una entidad o lista de entidades que coinciden con el criterio
     * @throws SQLiteException Si el campo no existe o hay errores en la consulta
     */
    public Object createFindBy(Method method, Object[] args) {
        String methodName = method.getName();

        String field = methodName.substring("findBy".length());
        String fieldLower = Character.toLowerCase(field.charAt(0)) + field.substring(1);

        String column = fieldToColumn.get(fieldLower);
        if (column == null)
            throw new SQLiteException("Field not found: " + fieldLower);

        String sql = "SELECT * FROM " + tableName + " WHERE " + column + " = ?";

        // Check the return type of the method
        Class<?> returnType = method.getReturnType();
        if (List.class.isAssignableFrom(returnType)) {
            return queryList(sql, args);
        } else {
            return queryItem(sql, createArgs(args));
        }
    }

    /**
     * Crea una consulta para buscar todas las entidades que coinciden con un valor específico de un campo.
     * Analiza el nombre del metodo para determinar el campo por el que buscar.
     * 
     * @param method El metodo invocado
     * @param args Los argumentos pasados al metodo (valor a buscar)
     * @return Una lista de entidades que coinciden con el criterio
     * @throws SQLiteException Si el campo no existe o hay errores en la consulta
     */
    public List<T> createFindAllBy(Method method, Object[] args) {
        String methodName = method.getName();

        String field = methodName.substring("findAllBy".length());
        String fieldLower = Character.toLowerCase(field.charAt(0)) + field.substring(1);

        String column = fieldToColumn.get(fieldLower);
        if (column == null)
            throw new SQLiteException("Field not found: " + fieldLower);

        String sql = "SELECT * FROM " + tableName + " WHERE " + column + " = ?";
        return queryList(sql, args);
    }

    /**
     * Retrieves all entities from the database.
     * 
     * @return A list of all entities
     */
    public List<T> findAll() {
        String sql = "SELECT * FROM " + tableName;
        return queryList(sql, new String[0]);
    }

    /**
     * Retrieves all entities ordered by a specific field.
     * The direction (ASC or DESC) is determined from the method name.
     * 
     * @param method The method being invoked
     * @return A list of all entities ordered by the specified field
     * @throws SQLiteException If the field doesn't exist or there's an error in the query
     */
    public List<T> findAllOrderBy(Method method) {
        String methodName = method.getName();

        // Extract the direction from the method name
        String direction = "ASC"; // Default to ASC
        if (methodName.endsWith("Desc")) {
            direction = "DESC";
            methodName = methodName.substring(0, methodName.length() - 4); // Remove "Desc"
        } else if (methodName.endsWith("Asc")) {
            methodName = methodName.substring(0, methodName.length() - 3); // Remove "Asc"
        }

        // Extract the field name
        String field = methodName.substring("findAllOrderBy".length());
        String fieldLower = Character.toLowerCase(field.charAt(0)) + field.substring(1);

        String column = fieldToColumn.get(fieldLower);
        if (column == null)
            throw new SQLiteException("Field not found: " + fieldLower);

        String sql = "SELECT * FROM " + tableName + " ORDER BY " + column + " " + direction;

        return queryList(sql, new String[0]);
    }

    /**
     * Retrieves all entities that match a specific field value, ordered by another field.
     * The direction (ASC or DESC) is determined from the method name.
     * 
     * @param method The method being invoked
     * @param args The arguments passed to the method (field value only)
     * @return A list of entities that match the criteria, ordered as specified
     * @throws SQLiteException If the fields don't exist or there's an error in the query
     */
    public List<T> findAllByFieldOrderByField(Method method, Object[] args) {
        String methodName = method.getName();

        // Extract the direction from the method name
        String direction = "ASC"; // Default to ASC
        if (methodName.endsWith("Desc")) {
            direction = "DESC";
            methodName = methodName.substring(0, methodName.length() - 4); // Remove "Desc"
        } else if (methodName.endsWith("Asc")) {
            methodName = methodName.substring(0, methodName.length() - 3); // Remove "Asc"
        }

        // Extract the field names from the method name
        String[] parts = methodName.split("OrderBy");
        if (parts.length != 2)
            throw new SQLiteException("Invalid method name format: " + methodName);

        String whereFieldPart = parts[0].substring("findAllBy".length());
        String orderFieldPart = parts[1];

        // Convert to camelCase for field lookup
        String whereFieldLower = Character.toLowerCase(whereFieldPart.charAt(0)) + whereFieldPart.substring(1);
        String orderFieldLower = Character.toLowerCase(orderFieldPart.charAt(0)) + orderFieldPart.substring(1);

        // Get the actual column names
        String whereColumn = fieldToColumn.get(whereFieldLower);
        if (whereColumn == null)
            throw new SQLiteException("Where field not found: " + whereFieldLower);

        String orderColumn = fieldToColumn.get(orderFieldLower);
        if (orderColumn == null)
            throw new SQLiteException("Order field not found: " + orderFieldLower);

        // Build the SQL query
        String sql = "SELECT * FROM " + tableName + " WHERE " + whereColumn + " = ? ORDER BY " + orderColumn + " " + direction;

        // Execute the query with just the first argument (the where value)
        return queryList(sql, createArgs(args));
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
                results.add(getResultCursor(cursor));

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
                return Optional.of(getResultCursor(cursor));
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
    public T getResultCursor(Cursor cursor) {
        try {
            T instance = entityClass.newInstance();

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
            Cursor cursorJoin = db.rawQuery(sql, new String[] { targetNameValue.toString() });
            if (cursorJoin.moveToFirst()) {
                // Create an instance of the relationship class
                Object relatedInstance = relationshipClass.newInstance();

                // Process all fields in the related entity
                for (Field relatedField : relationshipClass.getDeclaredFields()) {
                    if (relatedField.isAnnotationPresent(Column.class)) {
                        processColumnField(cursorJoin, relatedInstance, relatedField);
                    }
                }

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

                default:
                    throw new SQLiteException("Unsupported type for parameter: " + args[i].getClass().getSimpleName());
            }

    	}

    	return result;
    }




}
