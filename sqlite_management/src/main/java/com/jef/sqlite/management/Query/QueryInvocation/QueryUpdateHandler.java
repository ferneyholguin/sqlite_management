package com.jef.sqlite.management.Query.QueryInvocation;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import com.jef.sqlite.management.SQLiteManagement;
import com.jef.sqlite.management.exceptions.SQLiteException;
import com.jef.sqlite.management.interfaces.Column;
import com.jef.sqlite.management.interfaces.Join;
import com.jef.sqlite.management.interfaces.Table;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Handler class for update operations in the query system.
 * This class contains all methods related to updating entities.
 * 
 * @param <T> The entity type being updated
 */
public class QueryUpdateHandler<T> {

    private final Class<T> entityClass;
    private final SQLiteManagement management;
    private final String tableName;
    private final Map<String, String> fieldToColumn;

    /**
     * Constructor for QueryUpdateHandler
     * 
     * @param entityClass The entity class being updated
     * @param management The SQLiteManagement instance
     */
    public QueryUpdateHandler(Class<T> entityClass, SQLiteManagement management) {
        this.entityClass = entityClass;
        this.management = management;
        this.tableName = entityClass.getAnnotation(Table.class).name();
        this.fieldToColumn = new HashMap<>();

        for (Field field : entityClass.getDeclaredFields())
            if (field.isAnnotationPresent(Column.class))
                fieldToColumn.put(field.getName().toLowerCase(), field.getAnnotation(Column.class).name());
    }

    /**
     * Updates an entity in the database based on a specific field value.
     * Parses the method name to determine which field to use in the WHERE clause.
     * 
     * @param method The method being invoked
     * @param entity The entity with updated values
     * @param args The arguments for the WHERE clause
     * @return The number of rows affected
     * @throws SQLiteException If there's an error during the update operation
     */
    public int updateByField(Method method, T entity, String[] args) {
        if (entity == null)
            throw new SQLiteException("Entity cannot be null for update operation");

        String methodName = method.getName();

        // Extract the field name from the method name (e.g., "updateByName" -> "name")
        String field = methodName.substring("updateBy".length());
        String fieldLower = Character.toLowerCase(field.charAt(0)) + field.substring(1);

        // Get the corresponding column name
        String column = fieldToColumn.get(fieldLower);
        if (column == null)
            throw new SQLiteException("Field not found: " + fieldLower);

        // Create ContentValues from entity fields
        ContentValues values = createContentValuesFromEntity(entity);

        // Perform the update operation
        return updateEntity(values, column, args[0]);
    }

    /**
     * Creates ContentValues from an entity's fields.
     * 
     * @param entity The entity to extract values from
     * @return ContentValues containing the entity's field values
     * @throws SQLiteException If there's an error accessing the entity's fields
     */
    private ContentValues createContentValuesFromEntity(T entity) {
        ContentValues values = new ContentValues();

        // Process all fields with Column or Join annotation
        for (Field field : entityClass.getDeclaredFields()) {
            if (!field.isAnnotationPresent(Column.class) && !field.isAnnotationPresent(Join.class))
                continue;

            field.setAccessible(true);
            try {
                Object value = field.get(entity);

                // Skip null values
                if (value == null)
                    continue;

                if (field.isAnnotationPresent(Column.class)) {
                    Column column = field.getAnnotation(Column.class);
                    String columnName = column.name();

                    // Skip primary key fields for updates
                    if (column.isPrimaryKey())
                        continue;

                    // Add the value to ContentValues based on its type
                    if (value instanceof String) {
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
                } else if (field.isAnnotationPresent(Join.class)) {
                    Join join = field.getAnnotation(Join.class);
                    String columnName = join.targetName();

                    // Add the value to ContentValues based on its type
                    if (value instanceof String) {
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
            } catch (IllegalAccessException e) {
                throw new SQLiteException("Error accessing field " + field.getName() + ": " + e.getMessage(), e);
            }
        }

        return values;
    }

    /**
     * Performs the actual database update operation.
     * 
     * @param values The values to update
     * @param whereColumn The column to use in the WHERE clause
     * @param whereValue The value to use in the WHERE clause
     * @return The number of rows affected
     * @throws SQLiteException If there's an error during the update operation
     */
    private int updateEntity(ContentValues values, String whereColumn, String whereValue) {
        SQLiteDatabase db = management.getWritableDatabase();
        try {
            return db.update(tableName, values, whereColumn + " = ?", new String[]{whereValue});
        } catch (Exception e) {
            throw new SQLiteException("Error updating entity: " + e.getMessage(), e);
        } finally {
            db.close();
        }
    }


}
