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

                    // For Join fields, we need to extract the value from the related object
                    try {
                        // Get the relationship class
                        Class<?> relationshipClass = join.relationShip();

                        // Find the field in the relationship class that corresponds to the source attribute
                        String sourceFieldName = null;
                        for (Field relatedField : relationshipClass.getDeclaredFields()) {
                            if (relatedField.isAnnotationPresent(Column.class)) {
                                Column column = relatedField.getAnnotation(Column.class);
                                if (column.name().equals(join.source())) {
                                    sourceFieldName = relatedField.getName();
                                    break;
                                }
                            }
                        }

                        if (sourceFieldName == null) {
                            throw new SQLiteException("Source field not found in relationship class: " + join.source());
                        }

                        // Get the getter method for the source field
                        String getterMethodName = "get" + Character.toUpperCase(sourceFieldName.charAt(0)) + sourceFieldName.substring(1);
                        Method getterMethod = relationshipClass.getMethod(getterMethodName);

                        // Invoke the getter method on the value object to get the actual value
                        Object sourceValue = getterMethod.invoke(value);

                        // Add the source value to ContentValues based on its type
                        if (sourceValue instanceof String) {
                            values.put(columnName, (String) sourceValue);
                        } else if (sourceValue instanceof Integer) {
                            values.put(columnName, (Integer) sourceValue);
                        } else if (sourceValue instanceof Long) {
                            values.put(columnName, (Long) sourceValue);
                        } else if (sourceValue instanceof Double) {
                            values.put(columnName, (Double) sourceValue);
                        } else if (sourceValue instanceof Float) {
                            values.put(columnName, (Float) sourceValue);
                        } else if (sourceValue instanceof Boolean) {
                            values.put(columnName, ((Boolean) sourceValue) ? 1 : 0);
                        }
                    } catch (Exception e) {
                        throw new SQLiteException("Error extracting value from Join field: " + e.getMessage(), e);
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

    /**
     * Performs a database update operation with multiple WHERE conditions.
     * 
     * @param values The values to update
     * @param whereColumns The columns to use in the WHERE clause
     * @param whereValues The values to use in the WHERE clause
     * @return The number of rows affected
     * @throws SQLiteException If there's an error during the update operation
     */
    private int updateEntityWithMultipleConditions(ContentValues values, String[] whereColumns, Object[] whereValues) {
        if (whereColumns.length != whereValues.length) {
            throw new SQLiteException("Number of columns and values must match");
        }

        StringBuilder whereClause = new StringBuilder();
        String[] whereArgs = new String[whereValues.length];

        for (int i = 0; i < whereColumns.length; i++) {
            if (i > 0) {
                whereClause.append(" AND ");
            }
            whereClause.append(whereColumns[i]).append(" = ?");
            whereArgs[i] = String.valueOf(whereValues[i]);
        }

        SQLiteDatabase db = management.getWritableDatabase();
        try {
            return db.update(tableName, values, whereClause.toString(), whereArgs);
        } catch (Exception e) {
            throw new SQLiteException("Error updating entity: " + e.getMessage(), e);
        } finally {
            db.close();
        }
    }

    /**
     * Updates an entity in the database based on multiple field values.
     * Parses the method name to determine which field to update and which fields to use in the WHERE clause.
     * Method name format: update[FieldToUpdate]Where[Field1]And[Field2]And[Field3]...
     * 
     * @param method The method being invoked
     * @param args The arguments for the update operation (first arg is the value to set, remaining args are for WHERE conditions)
     * @return The number of rows affected
     * @throws SQLiteException If there's an error during the update operation
     */
    public int updateFieldWhereConditions(Method method, Object[] args) {
        if (args == null || args.length < 2) {
            throw new SQLiteException("At least one update value and one WHERE condition value are required");
        }

        String methodName = method.getName();

        // Check if the method name follows the expected pattern
        if (!methodName.startsWith("update") || !methodName.contains("Where")) {
            throw new SQLiteException("Invalid method name format. Expected: update[Field]Where[Condition1]And[Condition2]...");
        }

        // Extract the field to update from the method name
        String updateFieldPart = methodName.substring("update".length(), methodName.indexOf("Where"));
        String updateFieldName = Character.toLowerCase(updateFieldPart.charAt(0)) + updateFieldPart.substring(1);

        // Get the corresponding column name for the field to update
        String updateColumnName = fieldToColumn.get(updateFieldName);
        if (updateColumnName == null) {
            throw new SQLiteException("Update field not found: " + updateFieldName);
        }

        // Extract the WHERE conditions from the method name
        String whereConditionsPart = methodName.substring(methodName.indexOf("Where") + "Where".length());
        String[] whereFieldParts = whereConditionsPart.split("And");

        if (whereFieldParts.length != args.length - 1) {
            throw new SQLiteException("Number of WHERE conditions in method name does not match number of arguments");
        }

        // Get the corresponding column names for the WHERE fields
        String[] whereColumnNames = new String[whereFieldParts.length];
        for (int i = 0; i < whereFieldParts.length; i++) {
            String whereFieldName = Character.toLowerCase(whereFieldParts[i].charAt(0)) + whereFieldParts[i].substring(1);
            whereColumnNames[i] = fieldToColumn.get(whereFieldName);
            if (whereColumnNames[i] == null) {
                throw new SQLiteException("WHERE field not found: " + whereFieldName);
            }
        }

        // Create ContentValues with the single field to update
        ContentValues values = new ContentValues();
        Object updateValue = args[0];

        // Add the value to ContentValues based on its type
        if (updateValue instanceof String) {
            values.put(updateColumnName, (String) updateValue);
        } else if (updateValue instanceof Integer) {
            values.put(updateColumnName, (Integer) updateValue);
        } else if (updateValue instanceof Long) {
            values.put(updateColumnName, (Long) updateValue);
        } else if (updateValue instanceof Double) {
            values.put(updateColumnName, (Double) updateValue);
        } else if (updateValue instanceof Float) {
            values.put(updateColumnName, (Float) updateValue);
        } else if (updateValue instanceof Boolean) {
            values.put(updateColumnName, ((Boolean) updateValue) ? 1 : 0);
        } else {
            throw new SQLiteException("Unsupported type for update value: " + updateValue.getClass().getName());
        }

        // Extract the WHERE values from the arguments (skip the first argument which is the update value)
        Object[] whereValues = new Object[whereFieldParts.length];
        System.arraycopy(args, 1, whereValues, 0, whereFieldParts.length);

        // Perform the update operation with multiple WHERE conditions
        return updateEntityWithMultipleConditions(values, whereColumnNames, whereValues);
    }










}
