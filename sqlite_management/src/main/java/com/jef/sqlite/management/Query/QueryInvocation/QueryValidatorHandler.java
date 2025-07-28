package com.jef.sqlite.management.Query.QueryInvocation;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.jef.sqlite.management.SQLiteManagement;
import com.jef.sqlite.management.exceptions.SQLiteException;
import com.jef.sqlite.management.interfaces.Column;
import com.jef.sqlite.management.interfaces.Join;
import com.jef.sqlite.management.interfaces.Table;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Handler class for validating entities before database operations.
 * This class contains methods to check if entities are valid for insertion or update.
 * 
 * @param <T> The entity type being validated
 */
public class QueryValidatorHandler<T> {

    private final SQLiteManagement management;

    /**
     * Constructor for QueryValidatorHandler
     *
     * @param management The SQLiteManagement instance
     */
    public QueryValidatorHandler(SQLiteManagement management) {
        this.management = management;
    }

    /**
     * Validates if an entity is correct for database insertion.
     * Checks if:
     * 1. The entity is not null
     * 2. All non-null fields have values
     * 3. For unique fields, no existing records have the same values
     * 
     * @param entity The entity to validate
     * @return true if the entity is valid, false otherwise
     * @throws SQLiteException If there's an error during validation
     */
    public boolean validateEntity(T entity) throws SQLiteException {
        if (entity == null)
            throw new SQLiteException("Cannot validate null entity");

        Class<?> entityClass = entity.getClass();
        final String tableName = entityClass.getAnnotation(Table.class).name();

        if (!entityClass.isAnnotationPresent(Table.class))
            throw new SQLiteException("Entity class " + entityClass.getName() + " is not annotated with @Table");

        List<String> validationErrors = new ArrayList<>();

        // Check all fields with Column annotation
        for (Field field : entityClass.getDeclaredFields()) {
            if (!field.isAnnotationPresent(Column.class))
                continue;

            Column column = field.getAnnotation(Column.class);
            String columnName = column.name();
            String fieldName = field.getName();

            field.setAccessible(true);
            try {
                Object value = field.get(entity);

                // Check if field allows null
                if (!column.permitNull() && value == null) {
                    validationErrors.add("Field '" + fieldName + "' cannot be null");
                    continue;
                }

                // Skip null values for the rest of the checks
                if (value == null)
                    continue;

                // Check if field is unique and if there are existing records with the same value
                if (column.unique() && !column.autoIncrement())
                    if (existsWithSameValue(tableName, columnName, value))
                        validationErrors.add("Field '" + fieldName + "' must be unique. Value '" + value + "' already exists");

            } catch (IllegalAccessException e) {
                throw new SQLiteException("Error accessing field " + fieldName + ": " + e.getMessage(), e);
            }
        }

        // Check all fields with Join annotation
        for (Field field : entityClass.getDeclaredFields()) {
            if (!field.isAnnotationPresent(Join.class))
                continue;

            Join join = field.getAnnotation(Join.class);
            String fieldName = field.getName();

            field.setAccessible(true);
            try {
                Object relatedEntity = field.get(entity);

                // Check if field allows null
                if (!join.permitNull() && relatedEntity == null) {
                    validationErrors.add("Field '" + fieldName + "' with Join annotation cannot be null");
                    continue;
                }

                // Skip null values for the rest of the checks
                if (relatedEntity == null)
                    continue;

                // For Join fields, we need to extract the source field value from the related entity
                String source = join.source();
                String targetName = join.targetName();
                Object sourceValue = null;

                // Find the source field in the related entity
                for (Field relatedField : relatedEntity.getClass().getDeclaredFields())
                    if (relatedField.getName().equals(source)) {
                        relatedField.setAccessible(true);
                        sourceValue = relatedField.get(relatedEntity);
                        break;
                    }


                // Check if the join field is unique and if there are existing records with the same value
                if (join.unique() && sourceValue != null)
                    if (existsWithSameValue(tableName, targetName, sourceValue))
                        validationErrors.add("Field '" + fieldName + "' with Join annotation must be unique. Value '" + sourceValue + "' already exists");

            } catch (IllegalAccessException e) {
                throw new SQLiteException("Error accessing field " + fieldName + ": " + e.getMessage(), e);
            }
        }

        // If there are validation errors, throw an exception with all errors
        if (!validationErrors.isEmpty()) {
            throw new SQLiteException("Entity validation failed: " + String.join(", ", validationErrors));
        }

        return true;
    }

    /**
     * Checks if there are existing records with the same value for a specific column.
     * 
     * @param columnName The name of the column to check
     * @param value The value to check for
     * @return true if there are existing records, false otherwise
     * @throws SQLiteException If there's an error executing the query
     */
    private boolean existsWithSameValue(String tableName, String columnName, Object value) throws SQLiteException {
        SQLiteDatabase db = management.getReadableDatabase();

        try {
            String sql = "SELECT COUNT(*) FROM " + tableName + " WHERE " + columnName + " = ?";
            String[] args = new String[]{convertValueToString(value)};

            Cursor cursor = db.rawQuery(sql, args);
            boolean exists = false;

            if (cursor.moveToFirst())
                exists = cursor.getInt(0) > 0;

            cursor.close();
            return exists;
        } catch (Exception ex) {
            throw new SQLiteException("Error executing query: " + ex.getMessage(), ex);
        } finally {
            db.close();
        }
    }

    /**
     * Converts a value to a string for use in SQL queries.
     * 
     * @param value The value to convert
     * @return The string representation of the value
     * @throws SQLiteException If the value type is not supported
     */
    private String convertValueToString(Object value) throws SQLiteException {
        if (value == null)
            return null;

        String typeName = value.getClass().getSimpleName().toLowerCase();

        switch (typeName) {
            case "string":
            case "short":
            case "int":
            case "integer":
            case "long":
            case "double":
            case "float":
                return value.toString();
            case "boolean":
                return ((boolean) value) ? "1" : "0";
            default:
                throw new SQLiteException("Unsupported type for parameter: " + value.getClass().getSimpleName());
        }
    }







}
