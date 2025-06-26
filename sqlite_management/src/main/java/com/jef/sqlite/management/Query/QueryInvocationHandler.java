package com.jef.sqlite.management.Query;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.jef.sqlite.management.SQLiteManagement;
import com.jef.sqlite.management.exceptions.SQLiteException;
import com.jef.sqlite.management.interfaces.Column;
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

        String field = methodName.substring("findAllBy".length());
        String fieldLower = Character.toLowerCase(field.charAt(0)) + field.substring(1);

        String column = fieldToColumn.get(fieldLower);
        if (column == null)
            throw new SQLiteException("Field not found: " + fieldLower);


        String sql = "SELECT * FROM " + tableName + " WHERE " + column + " = ?";
        return queryItem(sql, args);
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

        for (Field field : entityClass.getDeclaredFields()) {
            if (!field.isAnnotationPresent(Column.class))
                continue;

            Column column = field.getAnnotation(Column.class);
            String columnName = column.name();
            int columnIndex = cursor.getColumnIndex(columnName);
            if (columnIndex == -1) continue;

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



}
