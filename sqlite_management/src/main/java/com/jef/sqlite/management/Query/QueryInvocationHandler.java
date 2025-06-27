package com.jef.sqlite.management.Query;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.jef.sqlite.management.SQLiteManagement;
import com.jef.sqlite.management.SQLiteTable;
import com.jef.sqlite.management.exceptions.SQLiteException;
import com.jef.sqlite.management.interfaces.Column;
import com.jef.sqlite.management.interfaces.DynamicQuery;
import com.jef.sqlite.management.interfaces.Join;
import com.jef.sqlite.management.interfaces.Table;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public class QueryInvocationHandler<T> implements InvocationHandler {

    private final Class<T> entityClass;
    private final SQLiteManagement management;
    private final String tableName;
    private final Map<String, String> fieldToColumn;

    public QueryInvocationHandler(Class<T> entityClass, SQLiteManagement management) {
        this.entityClass = entityClass;
        this.management = management;
        this.tableName = entityClass.getAnnotation(Table.class).name();
        this.fieldToColumn = new HashMap<>();

        for (Field field : entityClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(Column.class)) {
                fieldToColumn.put(field.getName().toLowerCase(), field.getAnnotation(Column.class).name());
            }
        }
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        String methodName = method.getName();

        if (methodName.equals("findAll")) {
            return findAll();
        } else if (methodName.equals("save")) {
            if (args == null || args.length == 0 || args[0] == null)
                throw new SQLiteException("Entity is required for save method");

            return save((T) args[0]);
        } else if (methodName.startsWith("findAllOrderBy")) {
            if (args == null || args.length == 0)
                throw new SQLiteException("Sort direction is required for findAllOrderBy methods");

            String[] argsString = Stream.of(args)
                    .filter(Objects::nonNull)
                    .map(String::valueOf)
                    .toArray(String[]::new);

            if (argsString.length == 0)
                throw new SQLiteException("Sort direction is required for findAllOrderBy methods");

            return findAllOrderBy(method, argsString[0]);
        } else if (methodName.matches("findAllBy\\w+OrderBy\\w+")) {
            // Handle methods like findAllByNameOrderById
            if (args == null || args.length < 2)
                throw new SQLiteException("Where value and sort direction are required for findAllBy[Field]OrderBy[Field] methods");

            String[] argsString = Stream.of(args)
                    .filter(Objects::nonNull)
                    .map(String::valueOf)
                    .toArray(String[]::new);

            if (argsString.length < 2)
                throw new SQLiteException("Where value and sort direction are required for findAllBy[Field]OrderBy[Field] methods");

            return findAllByFieldOrderByField(method, argsString);
        }

        if (args == null || args.length == 0)
            throw new SQLiteException("The args not have null o empty");

        String[] argsString = Stream.of(args)
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .toArray(String[]::new);

        if (argsString.length == 0)
            throw new SQLiteException("The args not is valid");

        if (methodName.startsWith("findBy"))
            return createFindBy(method, argsString);
        else if (methodName.startsWith("findAllBy") && !methodName.matches("findAllBy\\w+OrderBy\\w+"))
            return createFindAllBy(method, argsString);

        throw new UnsupportedOperationException("Method not supported: " + methodName);
    }

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

    public List<T> findAll() {
        String sql = "SELECT * FROM " + tableName;
        return queryList(sql, new String[0]);
    }

    public List<T> findAllOrderBy(Method method, String direction) {
        String methodName = method.getName();
        String fieldName = methodName.substring("findAllOrderBy".length());
        String fieldLower = Character.toLowerCase(fieldName.charAt(0)) + fieldName.substring(1);

        String column = fieldToColumn.get(fieldLower);
        if (column == null)
            throw new SQLiteException("Field not found: " + fieldLower);

        // Validate direction is either "ASC" or "DESC"
        String sortDirection = direction.trim().toUpperCase();
        if (!sortDirection.equals("ASC") && !sortDirection.equals("DESC"))
            throw new SQLiteException("Invalid sort direction: " + direction + ". Must be ASC or DESC");

        String sql = "SELECT * FROM " + tableName + " ORDER BY " + column + " " + sortDirection;
        return queryList(sql, new String[0]);
    }

    public List<T> findAllByFieldOrderByField(Method method, String[] args) {
        String methodName = method.getName();

        // Extract the field names from the method name
        // Format: findAllBy[WhereField]OrderBy[OrderField]
        String[] parts = methodName.split("OrderBy");
        if (parts.length != 2) {
            throw new SQLiteException("Invalid method name format. Expected: findAllBy[Field]OrderBy[Field]");
        }

        // Extract the WHERE field name
        String whereFieldName = parts[0].substring("findAllBy".length());
        String whereFieldLower = Character.toLowerCase(whereFieldName.charAt(0)) + whereFieldName.substring(1);

        // Extract the ORDER BY field name
        String orderFieldName = parts[1];
        String orderFieldLower = Character.toLowerCase(orderFieldName.charAt(0)) + orderFieldName.substring(1);

        // Get the corresponding column names
        String whereColumn = fieldToColumn.get(whereFieldLower);
        if (whereColumn == null)
            throw new SQLiteException("Field not found for WHERE clause: " + whereFieldLower);

        String orderColumn = fieldToColumn.get(orderFieldLower);
        if (orderColumn == null)
            throw new SQLiteException("Field not found for ORDER BY clause: " + orderFieldLower);

        // Get the WHERE value and sort direction
        String whereValue = args[0];
        String direction = args[1];

        // Validate direction is either "ASC" or "DESC"
        String sortDirection = direction.trim().toUpperCase();
        if (!sortDirection.equals("ASC") && !sortDirection.equals("DESC"))
            throw new SQLiteException("Invalid sort direction: " + direction + ". Must be ASC or DESC");

        // Create the SQL query with both WHERE and ORDER BY clauses
        String sql = "SELECT * FROM " + tableName + " WHERE " + whereColumn + " = ? ORDER BY " + orderColumn + " " + sortDirection;
        return queryList(sql, new String[]{whereValue});
    }



    public T getResultCursor(Cursor cursor) throws Exception {
        T instance = entityClass.getDeclaredConstructor().newInstance();

        // Process fields with Column annotation
        for (Field field : entityClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(Column.class)) {
                processColumnField(cursor, instance, field);
            }
        }

        // Process fields with Join annotation
        for (Field field : entityClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(Join.class)) {
                processJoinField(instance, field);
            }
        }

        return instance;
    }

    private void processColumnField(Cursor cursor, T instance, Field field) throws IllegalAccessException {
        Column column = field.getAnnotation(Column.class);
        String columnName = column.name();
        int columnIndex = cursor.getColumnIndex(columnName);
        if (columnIndex == -1) return;

        field.setAccessible(true);
        Class<?> type = field.getType();

        if (type == int.class || type == Integer.class) {
            field.set(instance, cursor.getInt(columnIndex));
        } else if (type == long.class || type == Long.class) {
            field.set(instance, cursor.getLong(columnIndex));
        } else if (type == float.class || type == Float.class) {
            field.set(instance, cursor.getFloat(columnIndex));
        } else if (type == double.class || type == Double.class) {
            field.set(instance, cursor.getDouble(columnIndex));
        } else if (type == String.class) {
            field.set(instance, cursor.getString(columnIndex));
        } else if (type == byte[].class) {
            field.set(instance, cursor.getBlob(columnIndex));
        } else if (type == boolean.class || type == Boolean.class) {
            field.set(instance, cursor.getInt(columnIndex) != 0);
        }
    }

    private void processJoinField(T instance, Field field) throws Exception {
        Join join = field.getAnnotation(Join.class);
        field.setAccessible(true);

        // Get the source field value from the instance
        Field sourceField = findFieldByColumnName(entityClass, join.source());
        if (sourceField == null)
            throw new SQLiteException("Source field not found: " + join.source());

        sourceField.setAccessible(true);
        Object sourceValue = sourceField.get(instance);

        // If source value is null and null is permitted, set field to null and return
        if (sourceValue == null) {
            if (join.permitNull()) {
                field.set(instance, null);
            } else {
                // Use default value if specified
                if (!join.defaultValue().isEmpty()) {
                    // Create an instance of the relationship class with default values
                    Object defaultInstance = createDefaultInstance(join.relationShip(), join.defaultValue());
                    field.set(instance, defaultInstance);
                }
            }
            return;
        }

        // Get the target table name
        Class<?> relationshipClass = join.relationShip();
        if (!relationshipClass.isAnnotationPresent(Table.class)) {
            throw new SQLiteException("Relationship class is not annotated with @Table: " + relationshipClass.getName());
        }

        Table tableAnnotation = relationshipClass.getAnnotation(Table.class);
        String targetTable = tableAnnotation.name();

        // Find the target field (primary key) in the relationship class
        Field targetField = findPrimaryKeyField(relationshipClass);
        if (targetField == null) {
            throw new SQLiteException("Primary key field not found in relationship class: " + relationshipClass.getName());
        }

        Column targetColumn = targetField.getAnnotation(Column.class);
        String targetColumnName = targetColumn.name();

        // Query the related entity
        String sql = "SELECT * FROM " + targetTable + " WHERE " + join.targetName() + " = ?";
        String[] selectionArgs = {sourceValue.toString()};

        SQLiteDatabase db = management.getReadableDatabase();
        try (Cursor cursor = db.rawQuery(sql, selectionArgs)) {
            if (cursor.moveToNext()) {
                // Create an instance of the relationship class
                Object relatedInstance = relationshipClass.getDeclaredConstructor().newInstance();

                // Process all fields with Column annotation in the relationship class
                for (Field relatedField : relationshipClass.getDeclaredFields()) {
                    if (!relatedField.isAnnotationPresent(Column.class)) continue;

                    Column column = relatedField.getAnnotation(Column.class);
                    String columnName = column.name();
                    int columnIndex = cursor.getColumnIndex(columnName);
                    if (columnIndex == -1) continue;

                    relatedField.setAccessible(true);
                    Class<?> type = relatedField.getType();

                    if (type == int.class || type == Integer.class) {
                        relatedField.set(relatedInstance, cursor.getInt(columnIndex));
                    } else if (type == long.class || type == Long.class) {
                        relatedField.set(relatedInstance, cursor.getLong(columnIndex));
                    } else if (type == float.class || type == Float.class) {
                        relatedField.set(relatedInstance, cursor.getFloat(columnIndex));
                    } else if (type == double.class || type == Double.class) {
                        relatedField.set(relatedInstance, cursor.getDouble(columnIndex));
                    } else if (type == String.class) {
                        relatedField.set(relatedInstance, cursor.getString(columnIndex));
                    } else if (type == byte[].class) {
                        relatedField.set(relatedInstance, cursor.getBlob(columnIndex));
                    } else if (type == boolean.class || type == Boolean.class) {
                        relatedField.set(relatedInstance, cursor.getInt(columnIndex) != 0);
                    }
                }

                // Set the related instance on the main entity
                field.set(instance, relatedInstance);
            } else if (!join.permitNull()) {
                // Use default value if specified
                if (!join.defaultValue().isEmpty()) {
                    // Create an instance of the relationship class with default values
                    Object defaultInstance = createDefaultInstance(join.relationShip(), join.defaultValue());
                    field.set(instance, defaultInstance);
                }
            }
        } finally {
            db.close();
        }
    }

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

    private Object createDefaultInstance(Class<?> clazz, String defaultValue) throws Exception {
        // Create a new instance of the class
        Object instance = clazz.getDeclaredConstructor().newInstance();

        // Find the primary key field
        Field primaryKeyField = findPrimaryKeyField(clazz);
        if (primaryKeyField != null) {
            primaryKeyField.setAccessible(true);

            // Set the default value based on the field type
            Class<?> type = primaryKeyField.getType();
            if (type == int.class || type == Integer.class) {
                primaryKeyField.set(instance, Integer.parseInt(defaultValue));
            } else if (type == long.class || type == Long.class) {
                primaryKeyField.set(instance, Long.parseLong(defaultValue));
            } else if (type == String.class) {
                primaryKeyField.set(instance, defaultValue);
            }
            // Add more type conversions as needed
        }

        return instance;
    }

    public List<T> queryList(String sql, String[] selectionArgs) {
        List<T> results = new ArrayList<>();
        SQLiteDatabase db = management.getReadableDatabase();

        try (Cursor cursor = db.rawQuery(sql, selectionArgs)) {
            while (cursor.moveToNext()) {
                T instance = getResultCursor(cursor);
                results.add(instance);
            }
        } catch(Exception e) {
            throw new SQLiteException("Error executing query: " + e.getMessage(), e);
        } finally {
            db.close();
        }

        return results;
    }

    public Optional<T> queryItem(String sql, String[] selectionArgs) {
        SQLiteDatabase db = management.getReadableDatabase();

        try (Cursor cursor = db.rawQuery(sql, selectionArgs)) {
            if (cursor.moveToNext())
                return Optional.of(getResultCursor(cursor));
        } catch (Exception ex) {
            throw new SQLiteException("Error executing query: " + ex.getMessage(), ex);
        } finally {
            db.close();
        }

        return Optional.empty();
    }

    /**
     * Saves an entity to the database. If the entity has a primary key value and exists in the database,
     * it will be updated. Otherwise, it will be inserted.
     * 
     * @param entity The entity to save
     * @return The saved entity with any auto-generated values (like auto-increment IDs)
     * @throws SQLiteException If there's an error during the save operation
     */
    public T save(T entity) throws SQLiteException {
        if (entity == null)
            throw new SQLiteException("Cannot save null entity");

        if (!entityClass.isAnnotationPresent(Table.class)) {
            throw new SQLiteException("Entity class " + entityClass.getName() + " is not annotated with @Table");
        }

        Table tableAnnotation = entityClass.getAnnotation(Table.class);
        String tableName = tableAnnotation.name();

        if (tableName == null || tableName.isEmpty()) {
            throw new SQLiteException("Entity class " + entityClass.getName() + " has no table name defined");
        }

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
        Object primaryKeyValue = null;

        // Process all fields with Column annotation
        for (Field field : entityClass.getDeclaredFields()) {
            if (!field.isAnnotationPresent(Column.class))
                continue;

            Column column = field.getAnnotation(Column.class);
            String columnName = column.name();

            if (columnName == null || columnName.isEmpty())
                continue;


            field.setAccessible(true);
            try {
                Object value = field.get(entity);

                // Skip null values
                if (value == null)
                    continue;

                // Store primary key information for later use
                if (column.isPrimaryKey()) {
                    primaryKeyField = field;
                    primaryKeyValue = value;

                    // Skip auto-increment primary keys for insert operations
                    if (column.isAutoIncrement() && isDefaultPrimaryKeyValue(value))
                        continue;

                }

                // Add value to ContentValues based on its type
                if (value instanceof String) {
                    values.put(columnName, (String) value);
                } else if (value instanceof Integer) {
                    values.put(columnName, (Integer) value);
                } else if (value instanceof Long) {
                    values.put(columnName, (Long) value);
                } else if (value instanceof Float) {
                    values.put(columnName, (Float) value);
                } else if (value instanceof Double) {
                    values.put(columnName, (Double) value);
                } else if (value instanceof Boolean) {
                    values.put(columnName, ((Boolean) value) ? 1 : 0);
                } else if (value instanceof byte[]) {
                    values.put(columnName, (byte[]) value);
                }
            } catch (IllegalAccessException e) {
                throw new SQLiteException("Error accessing field " + field.getName() + ": " + e.getMessage(), e);
            }
        }

        SQLiteDatabase db = management.getWritableDatabase();
        try {
            // Determine if this is an insert or update operation
            boolean isUpdate = false;

            if (primaryKeyField != null && primaryKeyValue != null && !isDefaultPrimaryKeyValue(primaryKeyValue)) {
                // Check if the entity exists in the database
                Column pkColumn = primaryKeyField.getAnnotation(Column.class);
                String pkColumnName = pkColumn.name();

                String[] columns = {pkColumnName};
                String selection = pkColumnName + " = ?";
                String[] selectionArgs = {primaryKeyValue.toString()};

                try (Cursor cursor = db.query(tableName, columns, selection, selectionArgs, null, null, null)) {
                    isUpdate = cursor.getCount() > 0;
                }
            }

            long id;
            if (isUpdate) {
                // Update existing entity
                Column pkColumn = primaryKeyField.getAnnotation(Column.class);
                String pkColumnName = pkColumn.name();

                String whereClause = pkColumnName + " = ?";
                String[] whereArgs = {primaryKeyValue.toString()};

                id = db.update(tableName, values, whereClause, whereArgs);
                if (id <= 0) {
                    throw new SQLiteException("Failed to update entity in table " + tableName);
                }
            } else {
                // Insert new entity
                id = db.insert(tableName, null, values);
                if (id == -1) {
                    throw new SQLiteException("Failed to insert entity into table " + tableName);
                }

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



}
