package com.jef.sqlite.management.Query.QueryInvocation;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import com.jef.sqlite.management.SQLiteManagement;
import com.jef.sqlite.management.exceptions.SQLiteException;
import com.jef.sqlite.management.interfaces.Column;
import com.jef.sqlite.management.interfaces.Join;
import com.jef.sqlite.management.interfaces.Table;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Handler class for save operations in the query system.
 * This class contains all methods related to saving entities.
 * 
 * @param <T> The entity type being saved
 */
public class QuerySaveHandler<T> {

    private final SQLiteManagement management;

    /**
     * Constructor for QuerySaveHandler
     *
     * @param management The SQLiteManagement instance
     */
    public QuerySaveHandler(SQLiteManagement management) {
        this.management = management;
    }

    /**
     * Saves an entity to the database by inserting it.
     * 
     * @param entity The entity to save
     * @return The row ID of the inserted record in the database
     * @throws SQLiteException If there's an error during the save operation
     */
    public long save(T entity) throws SQLiteException {
        if (entity == null)
            throw new SQLiteException("Cannot save null entity");

        Class<?> entityClass = entity.getClass();

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
                } else if (value instanceof Short) {
                    values.put(columnName, (Short) value);
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
                } else if (value instanceof byte[]) {
                    values.put(columnName, (byte[]) value);
                } else if (value instanceof Byte) {
                    values.put(columnName, (Byte) value);
                } else if (value instanceof Date) {
                    values.put(columnName, ((Date) value).getTime());
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
            } else if (value instanceof Short) {
                values.put(columnName, (Short) value);
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
            } else if (value instanceof byte[]) {
                values.put(columnName, (byte[]) value);
            } else if (value instanceof Byte) {
                values.put(columnName, (Byte) value);
            } else if (value instanceof Date) {
                values.put(columnName, ((Date) value).getTime());
            }

        }

        // Perform the database operation
        SQLiteDatabase db = management.getWritableDatabase();
        try {
            try {
                // Insert new entity
                long result = db.insert(tableName, null, values);

                return result;
            } catch (android.database.sqlite.SQLiteException e) {
                // Wrap Android's SQLiteException in our own SQLiteException
                throw new SQLiteException("SQLite error: " + e.getMessage(), e);
            }
        } finally {
            db.close();
        }
    }








}
