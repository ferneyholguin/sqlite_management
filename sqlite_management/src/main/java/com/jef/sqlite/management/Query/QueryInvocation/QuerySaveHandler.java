package com.jef.sqlite.management.Query.QueryInvocation;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import com.jef.sqlite.management.SQLiteManagement;
import com.jef.sqlite.management.exceptions.SQLiteException;
import com.jef.sqlite.management.interfaces.Column;
import com.jef.sqlite.management.interfaces.Join;
import com.jef.sqlite.management.interfaces.Table;

import java.lang.reflect.Field;
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

    /**
     * Constructor for QuerySaveHandler
     * 
     * @param entityClass The entity class being saved
     * @param management The SQLiteManagement instance
     */
    public QuerySaveHandler(Class<T> entityClass, SQLiteManagement management) {

        this.entityClass = entityClass;
        this.management = management;

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

        Map<String, Object> foreignKeyValues = new HashMap<>();

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

                if (relatedEntity == null)
                    continue;

                String source = join.source();

                //Se recorren todos los fields del objeto hasta encontrar el que corresponde con el source
                for (Field relatedField : relatedEntity.getClass().getDeclaredFields())
                    // No longer check for @Column annotation since source refers to the variable name, not the column name
                    if (relatedField.getName().equalsIgnoreCase(source)) {
                        relatedField.setAccessible(true);
                        foreignKeyValues.put(join.targetName(), relatedField.get(relatedEntity));
                        break;
                    }


            } catch (IllegalAccessException e) {
                throw new SQLiteException("Error accessing field " + field.getName() + ": " + e.getMessage(), e);
            }
        }

        // Create ContentValues from entity fields
        ContentValues values = new ContentValues();

        // Process all fields with Column annotation
        for (Field field : entityClass.getDeclaredFields()) {
            if (!field.isAnnotationPresent(Column.class))
                continue;

            Column column = field.getAnnotation(Column.class);
            String columnName = column.name();

            if (column.autoIncrement())
                continue;


            field.setAccessible(true);
            try {
                Object value = field.get(entity);

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

        // Add foreign key values from the map
        for (Map.Entry<String, Object> entry : foreignKeyValues.entrySet()) {
            String columnName = entry.getKey();
            Object value = entry.getValue();

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
        }

        // Perform the database operation
        SQLiteDatabase db = management.getWritableDatabase();
        try {
            try {
                // Insert new entity
                long id = db.insert(tableName, null, values);
                if (id == -1)
                    throw new SQLiteException("Failed to insert entity into table " + tableName);

                // Update the ID in the entity
                try {
                    // Find the ID field (with @Column annotation and autoIncrement=true)
                    for (Field field : entityClass.getDeclaredFields()) {
                        if (!field.isAnnotationPresent(Column.class))
                            continue;

                        Column column = field.getAnnotation(Column.class);
                        if (!column.autoIncrement())
                            continue;

                        field.setAccessible(true);
                        // Set the ID value in the entity
                        if (field.getType() == int.class || field.getType() == Integer.class) {
                            field.set(entity, (int) id);
                        } else {
                            field.set(entity, id);
                        }
                        break;

                    }
                } catch (IllegalAccessException e) {
                    throw new SQLiteException("Error setting ID in entity: " + e.getMessage(), e);
                }

                return entity;
            } catch (android.database.sqlite.SQLiteException e) {
                // Wrap Android's SQLiteException in our own SQLiteException
                throw new SQLiteException("SQLite error: " + e.getMessage(), e);
            }
        } finally {
            db.close();
        }
    }








}
