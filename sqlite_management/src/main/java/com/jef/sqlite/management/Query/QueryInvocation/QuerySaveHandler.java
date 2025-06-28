package com.jef.sqlite.management.Query.QueryInvocation;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.jef.sqlite.management.SQLiteManagement;
import com.jef.sqlite.management.exceptions.SQLiteException;
import com.jef.sqlite.management.interfaces.Column;
import com.jef.sqlite.management.interfaces.DynamicQuery;
import com.jef.sqlite.management.interfaces.Join;
import com.jef.sqlite.management.interfaces.Table;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Handler class for save operations in the query system.
 * This class contains all methods related to saving entities.
 * 
 * @param <T> The entity type being saved
 */
public class QuerySaveHandler<T> {
    
    private final Class<T> entityClass;
    private final SQLiteManagement management;
    private final String tableName;
    private final Map<String, String> fieldToColumn;

    /**
     * Constructor for QuerySaveHandler
     * 
     * @param entityClass The entity class being saved
     * @param management The SQLiteManagement instance
     */
    public QuerySaveHandler(Class<T> entityClass, SQLiteManagement management) {

        this.entityClass = entityClass;
        this.management = management;
        this.tableName = entityClass.getAnnotation(Table.class).name();
        this.fieldToColumn = new HashMap<>();

        for (Field field : entityClass.getDeclaredFields())
            if (field.isAnnotationPresent(Column.class))
                fieldToColumn.put(field.getName().toLowerCase(), field.getAnnotation(Column.class).name());
    }

    /**
     * Saves an entity to the database by inserting it.
     * 
     * @param entity The entity to save
     * @return The saved entity with any auto-generated values (like auto-increment IDs)
     * @throws SQLiteException If there's an error during the save operation
     */
    public T save(T entity) throws SQLiteException {
        if (entity == null)
            throw new SQLiteException("Cannot save null entity");

        if (!entityClass.isAnnotationPresent(Table.class))
            throw new SQLiteException("Entity class " + entityClass.getName() + " is not annotated with @Table");

        Table tableAnnotation = entityClass.getAnnotation(Table.class);
        String tableName = tableAnnotation.name();

        if (tableName == null || tableName.isEmpty())
            throw new SQLiteException("Entity class " + entityClass.getName() + " has no table name defined");

        // Process fields with Join annotation first to ensure related entities are saved
        // and their IDs are available for the foreign keys
        for (Field field : entityClass.getDeclaredFields()) {
            if (!field.isAnnotationPresent(Join.class))
                continue;

            Join join = field.getAnnotation(Join.class);
            field.setAccessible(true);

            try {
                // Get the related entity from the field
                Object relatedEntity = field.get(entity);

                // Skip if the related entity is null and null is permitted
                if (relatedEntity == null) {
                    if (join.permitNull()) {
                        continue;
                    } else if (!join.defaultValue().isEmpty()) {
                        // Use default value if specified
                        try {
                            relatedEntity = createDefaultInstance(join.relationShip(), join.defaultValue());
                            field.set(entity, relatedEntity);
                        } catch (Exception e) {
                            throw new SQLiteException("Error creating default instance: " + e.getMessage(), e);
                        }
                    } else {
                        continue;
                    }
                }

                // Find the source field in the current entity
                Field sourceField = findFieldByColumnName(entityClass, join.source());
                if (sourceField == null)
                    throw new SQLiteException("Source field not found: " + join.source());

                // Find the target field (primary key) in the relationship class
                Class<?> relationshipClass = join.relationShip();
                Field targetField = findPrimaryKeyField(relationshipClass);
                if (targetField == null)
                    throw new SQLiteException("Primary key field not found in relationship class: " + relationshipClass.getName());

                // Save the related entity to ensure it has a valid ID
                // This is done by creating a dynamic proxy for the relationship class
                // and calling its save method
                try {
                    // Create a dynamic proxy for the relationship class
                    Object relationshipProxy = java.lang.reflect.Proxy.newProxyInstance(
                            relationshipClass.getClassLoader(),
                            new Class<?>[]{DynamicQuery.class},
                            new QueryInvocationHandler<>(relationshipClass, management)
                    );

                    // Call the save method on the proxy
                    Method saveMethod = DynamicQuery.class.getMethod("save", Object.class);
                    Object savedRelatedEntity = saveMethod.invoke(relationshipProxy, relatedEntity);

                    // Update the related entity in the main entity
                    field.set(entity, savedRelatedEntity);

                    // Get the primary key value from the saved related entity
                    targetField.setAccessible(true);
                    Object targetValue = targetField.get(savedRelatedEntity);

                    // Set the foreign key value in the source field
                    sourceField.setAccessible(true);
                    sourceField.set(entity, targetValue);

                    // Log the foreign key value for debugging
                    System.out.println("[DEBUG_LOG] Setting foreign key value: " + targetValue + " in field: " + sourceField.getName());
                } catch (Exception e) {
                    throw new SQLiteException("Error saving related entity: " + e.getMessage(), e);
                }
            } catch (IllegalAccessException e) {
                throw new SQLiteException("Error accessing field " + field.getName() + ": " + e.getMessage(), e);
            }
        }

        // Create ContentValues from entity fields
        ContentValues values = new ContentValues();
        Field primaryKeyField = null;

        // Process all fields with Column annotation
        for (Field field : entityClass.getDeclaredFields()) {
            if (!field.isAnnotationPresent(Column.class))
                continue;

            Column column = field.getAnnotation(Column.class);
            String columnName = column.name();

            field.setAccessible(true);
            try {
                Object value = field.get(entity);

                // Skip null values for non-primary key fields
                if (value == null && !column.isPrimaryKey())
                    continue;

                // Remember the primary key field and value for later
                if (column.isPrimaryKey()) {
                    primaryKeyField = field;

                    // Skip auto-increment primary keys with default values for inserts
                    if (column.isAutoIncrement() && isDefaultPrimaryKeyValue(value))
                        continue;
                }

                // Add the value to ContentValues based on its type
                if (value == null) {
                    values.putNull(columnName);
                } else if (value instanceof String) {
                    values.put(columnName, (String) value);
                } else if (value instanceof Integer) {
                    values.put(columnName, (Integer) value);
                } else if (value instanceof Long) {
                    values.put(columnName, (Long) value);
                } else if (value instanceof Double) {
                    values.put(columnName, (Double) value);
                } else if (value instanceof Float) {
                    values.put(columnName, (Float) value);
                } else if (value instanceof Boolean) {
                    values.put(columnName, ((Boolean) value) ? 1 : 0);
                }
            } catch (IllegalAccessException e) {
                throw new SQLiteException("Error accessing field " + field.getName() + ": " + e.getMessage(), e);
            }
        }

        // Perform the database operation
        SQLiteDatabase db = management.getWritableDatabase();
        try {
            // Insert new entity
            long id = db.insert(tableName, null, values);
            if (id == -1)
                throw new SQLiteException("Failed to insert entity into table " + tableName);

            // If we have an auto-increment primary key, set its value in the entity
            if (primaryKeyField != null && primaryKeyField.getAnnotation(Column.class).isAutoIncrement()) {
                primaryKeyField.setAccessible(true);
                try {
                    if (primaryKeyField.getType() == int.class || primaryKeyField.getType() == Integer.class) {
                        primaryKeyField.set(entity, (int) id);
                    } else if (primaryKeyField.getType() == long.class || primaryKeyField.getType() == Long.class) {
                        primaryKeyField.set(entity, id);
                    }
                } catch (IllegalAccessException e) {
                    throw new SQLiteException("Error setting auto-generated ID: " + e.getMessage(), e);
                }
            }

            return entity;
        } finally {
            db.close();
        }
    }

    /**
     * Checks if a primary key value is the default value (0 for numeric types)
     * 
     * @param value The primary key value to check
     * @return true if the value is the default value, false otherwise
     */
    private boolean isDefaultPrimaryKeyValue(Object value) {
        if (value instanceof Integer) {
            return (Integer) value == 0;
        } else if (value instanceof Long) {
            return (Long) value == 0L;
        }
        return false;
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
