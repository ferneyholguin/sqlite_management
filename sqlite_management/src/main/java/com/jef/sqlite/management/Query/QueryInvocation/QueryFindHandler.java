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
     * Analiza el nombre del método para determinar el campo por el que buscar.
     * 
     * @param method El método invocado
     * @param args Los argumentos pasados al método (valor a buscar)
     * @return Una entidad o lista de entidades que coinciden con el criterio
     * @throws SQLiteException Si el campo no existe o hay errores en la consulta
     */
    public Object createFindBy(Method method, String[] args) {
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
            return queryItem(sql, args);
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
    public Object createFindAllBy(Method method, String[] args) {
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
     * 
     * @param method The method being invoked
     * @param direction The sort direction ("asc" or "desc")
     * @return A list of all entities ordered by the specified field
     * @throws SQLiteException If the field doesn't exist or there's an error in the query
     */
    public List<T> findAllOrderBy(Method method, String direction) {
        String methodName = method.getName();
        String field = methodName.substring("findAllOrderBy".length());
        String fieldLower = Character.toLowerCase(field.charAt(0)) + field.substring(1);

        String column = fieldToColumn.get(fieldLower);
        if (column == null)
            throw new SQLiteException("Field not found: " + fieldLower);

        String dir = direction.equalsIgnoreCase("desc") ? "DESC" : "ASC";
        String sql = "SELECT * FROM " + tableName + " ORDER BY " + column + " " + dir;

        return queryList(sql, new String[0]);
    }

    /**
     * Retrieves all entities that match a specific field value, ordered by another field.
     * 
     * @param method The method being invoked
     * @param args The arguments passed to the method (field value and sort direction)
     * @return A list of entities that match the criteria, ordered as specified
     * @throws SQLiteException If the fields don't exist or there's an error in the query
     */
    public List<T> findAllByFieldOrderByField(Method method, String[] args) {
        String methodName = method.getName();

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

        // Determine sort direction
        String direction = args[1].equalsIgnoreCase("desc") ? "DESC" : "ASC";

        // Build the SQL query
        String sql = "SELECT * FROM " + tableName + " WHERE " + whereColumn + " = ? ORDER BY " + orderColumn + " " + direction;

        // Execute the query with just the first argument (the where value)
        return queryList(sql, new String[]{args[0]});
    }

    /**
     * Executes a SQL query and returns a list of entities.
     * 
     * @param sql The SQL query to execute
     * @param selectionArgs The arguments for the query
     * @return A list of entities that match the query
     * @throws SQLiteException If there's an error executing the query
     */
    public List<T> queryList(String sql, String[] selectionArgs) {
        SQLiteDatabase db = management.getReadableDatabase();
        List<T> results = new ArrayList<>();

        try {
            Cursor cursor = db.rawQuery(sql, selectionArgs);
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

            for (Field field : entityClass.getDeclaredFields()) {
                if (field.isAnnotationPresent(Column.class)) {
                    processColumnField(cursor, instance, field);
                } else if (field.isAnnotationPresent(Join.class)) {
                    processJoinField(instance, field);
                }
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
    private void processColumnField(Cursor cursor, T instance, Field field) throws Exception {
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

        if (fieldType == String.class) {
            field.set(instance, cursor.getString(columnIndex));
        } else if (fieldType == int.class || fieldType == Integer.class) {
            field.set(instance, cursor.getInt(columnIndex));
        } else if (fieldType == long.class || fieldType == Long.class) {
            field.set(instance, cursor.getLong(columnIndex));
        } else if (fieldType == double.class || fieldType == Double.class) {
            field.set(instance, cursor.getDouble(columnIndex));
        } else if (fieldType == float.class || fieldType == Float.class) {
            field.set(instance, cursor.getFloat(columnIndex));
        } else if (fieldType == boolean.class || fieldType == Boolean.class) {
            field.set(instance, cursor.getInt(columnIndex) == 1);
        }
    }

    /**
     * Processes a field annotated with @Join, loading the related entity.
     * 
     * @param instance The entity instance
     * @param field The field to process
     * @throws Exception If there's an error processing the field
     */
    private void processJoinField(T instance, Field field) throws Exception {
        Join join = field.getAnnotation(Join.class);
        Class<?> relationshipClass = join.relationShip();

        // Find the source field (foreign key) in the current entity
        Field sourceField = findFieldByColumnName(entityClass, join.source());
        if (sourceField == null)
            return;

        // Find the target field (primary key) in the relationship class
        Field targetField = findPrimaryKeyField(relationshipClass);
        if (targetField == null)
            return;

        // Get the foreign key value from the source field
        sourceField.setAccessible(true);
        Object foreignKeyValue = sourceField.get(instance);

        // If the foreign key is null and null is permitted, leave the join field as null
        if (foreignKeyValue == null) {
            if (join.permitNull()) {
                return;
            } else if (!join.defaultValue().isEmpty()) {
                // Use default value if specified
                try {
                    Object defaultInstance = createDefaultInstance(relationshipClass, join.defaultValue());
                    field.setAccessible(true);
                    field.set(instance, defaultInstance);
                } catch (Exception e) {
                    throw new SQLiteException("Error creating default instance: " + e.getMessage(), e);
                }
                return;
            } else {
                return;
            }
        }

        // Get the column name for the target field
        Column targetColumn = targetField.getAnnotation(Column.class);
        String targetColumnName = targetColumn.name();

        // Create a query to find the related entity
        String sql = "SELECT * FROM " + relationshipClass.getAnnotation(Table.class).name() +
                " WHERE " + targetColumnName + " = ?";

        // Execute the query
        SQLiteDatabase db = management.getReadableDatabase();
        try {
            Cursor cursor = db.rawQuery(sql, new String[]{foreignKeyValue.toString()});
            if (cursor.moveToFirst()) {
                // Create an instance of the relationship class
                Object relatedInstance = relationshipClass.newInstance();

                // Process all fields in the related entity
                for (Field relatedField : relationshipClass.getDeclaredFields()) {
                    if (relatedField.isAnnotationPresent(Column.class)) {
                        Column column = relatedField.getAnnotation(Column.class);
                        String columnName = column.name();
                        int columnIndex = cursor.getColumnIndex(columnName);

                        if (columnIndex == -1)
                            continue;

                        relatedField.setAccessible(true);
                        Class<?> fieldType = relatedField.getType();

                        if (cursor.isNull(columnIndex)) {
                            relatedField.set(relatedInstance, null);
                            continue;
                        }

                        if (fieldType == String.class) {
                            relatedField.set(relatedInstance, cursor.getString(columnIndex));
                        } else if (fieldType == int.class || fieldType == Integer.class) {
                            relatedField.set(relatedInstance, cursor.getInt(columnIndex));
                        } else if (fieldType == long.class || fieldType == Long.class) {
                            relatedField.set(relatedInstance, cursor.getLong(columnIndex));
                        } else if (fieldType == double.class || fieldType == Double.class) {
                            relatedField.set(relatedInstance, cursor.getDouble(columnIndex));
                        } else if (fieldType == float.class || fieldType == Float.class) {
                            relatedField.set(relatedInstance, cursor.getFloat(columnIndex));
                        } else if (fieldType == boolean.class || fieldType == Boolean.class) {
                            relatedField.set(relatedInstance, cursor.getInt(columnIndex) == 1);
                        }
                    }
                }

                // Set the related instance in the main entity
                field.setAccessible(true);
                field.set(instance, relatedInstance);
            }
            cursor.close();
        } finally {
            db.close();
        }
    }

    /**
     * Finds a field in a class by its column name.
     * 
     * @param clazz The class to search in
     * @param columnName The column name to look for
     * @return The field with the matching column name, or null if not found
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

    /**
     * Finds the primary key field in a class.
     * 
     * @param clazz The class to search in
     * @return The primary key field, or null if not found
     */
    private Field findPrimaryKeyField(Class<?> clazz) {
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(Column.class)) {
                Column column = field.getAnnotation(Column.class);
                if (column.isPrimaryKey()) {
                    return field;
                }
            }
        }
        return null;
    }

    /**
     * Creates a default instance of a class with a specified value.
     * 
     * @param clazz The class to create an instance of
     * @param defaultValue The default value to set
     * @return A new instance with the default value set
     * @throws Exception If there's an error creating the instance
     */
    private Object createDefaultInstance(Class<?> clazz, String defaultValue) throws Exception {
        Object instance = clazz.newInstance();

        // Find the primary key field
        Field pkField = findPrimaryKeyField(clazz);
        if (pkField == null)
            throw new SQLiteException("No primary key field found in class: " + clazz.getName());

        pkField.setAccessible(true);
        Class<?> fieldType = pkField.getType();

        // Set the default value based on the field type
        if (fieldType == String.class) {
            pkField.set(instance, defaultValue);
        } else if (fieldType == int.class || fieldType == Integer.class) {
            pkField.set(instance, Integer.parseInt(defaultValue));
        } else if (fieldType == long.class || fieldType == Long.class) {
            pkField.set(instance, Long.parseLong(defaultValue));
        } else if (fieldType == double.class || fieldType == Double.class) {
            pkField.set(instance, Double.parseDouble(defaultValue));
        } else if (fieldType == float.class || fieldType == Float.class) {
            pkField.set(instance, Float.parseFloat(defaultValue));
        } else if (fieldType == boolean.class || fieldType == Boolean.class) {
            pkField.set(instance, Boolean.parseBoolean(defaultValue));
        }

        return instance;
    }




}