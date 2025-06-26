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
import java.util.stream.Collectors;
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
        if (args == null || args.length == 0)
            throw new SQLiteException("The args not have null o empty");

        String[] argsString = Stream.of(args)
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .toArray(String[]::new);

        if (argsString.length == 0)
            throw new SQLiteException("The args not is valid");

        String methodName = method.getName();

        if (methodName.startsWith("findBy"))
            return createFindBy(method, argsString);
        else if (methodName.startsWith("findAllBy"))
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



    public T getResultCursor(Cursor cursor) throws Exception {
        T instance = entityClass.getDeclaredConstructor().newInstance();

        for (Field field : entityClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(Column.class)) {
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
