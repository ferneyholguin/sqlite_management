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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QueryUpdateHandler<T> {

    private final Class<T> entityClass;
    private final SQLiteManagement management;
    private final String tableName;
    private final Map<String, String> fieldToColumn;

    /**
     * Constructor para QueryUpdateHandler.
     * Inicializa el manejador con la clase de entidad y el gestor de base de datos.
     *
     * @param entityClass La clase de entidad asociada a la consulta
     * @param management El gestor de la base de datos SQLite
     */
    public QueryUpdateHandler(Class<T> entityClass, SQLiteManagement management) {
        this.entityClass = entityClass;
        this.management = management;

        if (entityClass.isAnnotationPresent(Table.class))
            this.tableName = entityClass.getAnnotation(Table.class).name();
        else
            throw new IllegalArgumentException("Entity class " + entityClass.getName() + " is not annotated with @Table");

        this.fieldToColumn = new HashMap<>();

        for (Field field : entityClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(Column.class)) {
                String fieldName = Character.toLowerCase(field.getName().charAt(0)) + field.getName().substring(1);
                fieldToColumn.put(fieldName, field.getAnnotation(Column.class).name());
            } else if (field.isAnnotationPresent(Join.class)) {
                String fieldName = Character.toLowerCase(field.getName().charAt(0)) + field.getName().substring(1);
                fieldToColumn.put(fieldName, field.getAnnotation(Join.class).targetName());
            }
        }
    }

    public int updateBy(Method method, Object[] args) {
        if (args == null || args.length < 1)
            throw new SQLiteException("No arguments provided for updateBy");

        if (!(args[0] instanceof ContentValues))
            throw new SQLiteException("ContentValues and at least one where clause value are required");

        ContentValues values = (ContentValues) args[0];
        if (values.size() == 0)
            throw new SQLiteException("ContentValues cannot be empty");

        String methodName = method.getName();
        if (!methodName.startsWith("updateBy"))
            throw new SQLiteException("Method name must start with 'updateBy'");

        int startIndex = "updateBy".length();
        int endIndex = methodName.length();

        String whereString = methodName.substring(startIndex, endIndex);

        String whereClause = extractWhereClause(whereString);

        // Create whereArgs from args, skipping the ContentValues
        Object[] whereArgsObjects = new Object[args.length - 1];
        for (int i = 1; i < args.length; i++)
            whereArgsObjects[i - 1] = args[i];

        String[] whereArgs = createArgs(whereArgsObjects);

        SQLiteDatabase db = management.getWritableDatabase();
        try {
            return db.update(tableName, values, whereClause, whereArgs);
        } catch (android.database.sqlite.SQLiteException e) {
            throw new SQLiteException("Error updating entity: " + e.getMessage(), e);
        } finally {
            if (db != null && db.isOpen()) {
                db.close();
            }
        }

    }

    public int update(Method method, Object[] args) {
        String methodName = method.getName();

        if (!methodName.startsWith("update"))
            throw new SQLiteException("Method name must start with 'update'");

        String[] parts = splitCamelCase(methodName.substring("update".length()));

        if (parts.length < 1)
            throw new SQLiteException("No fields to update specified");

        int lastBy = 0;
        for(int i = parts.length-1; i >= 0; i--)
            if(parts[i].equalsIgnoreCase("by")) {
                lastBy = i;
                break;
            }

        final String whereClause;
        if (lastBy > 0) {
            StringBuilder whereString = new StringBuilder();
            for (int i = lastBy + 1; i < parts.length; i++)
                whereString.append(parts[i]);

            whereClause = extractWhereClause(whereString.toString());
        } else
            whereClause = null;

        final String[] partsFiltered;
        if (lastBy > 0) {
            partsFiltered = new String[lastBy];
            for (int i = 0; i < lastBy; i++)
                partsFiltered[i] = parts[i];
        } else
            partsFiltered = parts;


        String[] columnsToUpdate = extractColumnsToUpdate(partsFiltered);

        if (args.length < columnsToUpdate.length)
            throw new SQLiteException("Not enough arguments provided for update");

        ContentValues contentValues = new ContentValues();

        for (int i = 0; i < columnsToUpdate.length; i++) {
            if (args[i] == null) {
                contentValues.putNull(columnsToUpdate[i]);
                continue;
            }

            if (args[i] instanceof Boolean) {
                contentValues.put(columnsToUpdate[i], (Boolean) args[i] ? 1 : 0);
            } else if (args[i] instanceof Short) {
                contentValues.put(columnsToUpdate[i], (Short) args[i]);
            } else if (args[i] instanceof Integer) {
                contentValues.put(columnsToUpdate[i], (Integer) args[i]);
            } else if (args[i] instanceof Long) {
                contentValues.put(columnsToUpdate[i], (Long) args[i]);
            } else if (args[i] instanceof Float) {
                contentValues.put(columnsToUpdate[i], (Float) args[i]);
            } else if (args[i] instanceof Double) {
                contentValues.put(columnsToUpdate[i], (Double) args[i]);
            } else if (args[i] instanceof String) {
                contentValues.put(columnsToUpdate[i], (String) args[i]);
            } else if (args[i] instanceof byte[]) {
                contentValues.put(columnsToUpdate[i], (byte[]) args[i]);
            } else if (args[i] instanceof Byte) {
                contentValues.put(columnsToUpdate[i], (Byte) args[i]);
            } else if (args[i] instanceof Date) {
                contentValues.put(columnsToUpdate[i], ((Date) args[i]).getTime());
            } else
                throw new SQLiteException("Unsupported type for parameter: " + args[i].getClass().getSimpleName());

        }

        String[] whereArgs = createArgs(args, columnsToUpdate.length);

        SQLiteDatabase db = management.getWritableDatabase();
        try {
            return db.update(tableName, contentValues, whereClause, whereArgs);
        } catch (android.database.sqlite.SQLiteException e) {
            throw new SQLiteException("Error updating entity: " + e.getMessage(), e);
        } finally {
            if (db != null && db.isOpen()) {
                db.close();
            }
        }
    }


    /**
     * Extracts the where clause from a method name.
     * <p>
     * for example: "AgeAndName" would return "age = ? AND name = ?" <br>
     * "FirstNameAndLastName" would return "firstName = ? AND lastName = ?" <br>
     * "AgeOrName" would return "age = ? OR name = ?" <br>
     * "AgeAndNameAndLastName" would return "age = ? AND name = ? AND lastName = ?" <br>
     * "AgeOrNameOrLastName" would return "age = ? OR name = ? OR lastName = ?" <br>
     *
     * @param whereString The WhereString after "By"
     * @return The where clause
     */
    public String extractWhereClause(String whereString) {
        //Se separan las palabras por camelCase incluido los And y Or
        String[] whereCamelCase = splitCamelCase(whereString);

        List<String> parts = new ArrayList<>();

        StringBuilder currentWord = new StringBuilder();
        for (String part : whereCamelCase) {
            if (part.equalsIgnoreCase("and") || part.equalsIgnoreCase("or")) {
                if (currentWord.length() > 0)
                    parts.add(currentWord.toString());

                parts.add(part);
                currentWord = new StringBuilder();
            } else
                currentWord.append(part);
        }

        // Add the last word if it's not empty
        if (currentWord.length() > 0)
            parts.add(currentWord.toString());

        StringBuilder whereClause = new StringBuilder();

        for (String part : parts) {
            if (part.equalsIgnoreCase("and") || part.equalsIgnoreCase("or"))
                whereClause.append(" ").append(part.toUpperCase());
            else {
                String fieldName = Character.toLowerCase(part.charAt(0)) + part.substring(1);
                if (!fieldToColumn.containsKey(fieldName))
                    throw new SQLiteException("Field not found: " + fieldName);

                String columnName = fieldToColumn.get(fieldName);

                whereClause.append(" ").append(columnName).append(" = ?");
            }
        }

        return whereClause.toString();
    }

    public String[] extractColumnsToUpdate(String[] parts) {
        List<String> fieldsToUpdate = new ArrayList<>();

        StringBuilder currentWord = new StringBuilder();
        for (String part : parts) {
            if (currentWord.length() <= 0)
                currentWord
                        .append(Character.toLowerCase(part.charAt(0)))
                        .append(part.substring(1).toLowerCase());
            else
                currentWord.append(part);

            if (fieldsToUpdate.contains(currentWord.toString()))
                continue;

            if (fieldToColumn.containsKey(currentWord.toString())) {
                fieldsToUpdate.add(currentWord.toString());
                currentWord = new StringBuilder();
                continue;
            }

            List<String> optionalSearch = new ArrayList<>();
            optionalSearch.add(Character.toUpperCase(currentWord.charAt(0)) + currentWord.substring(1));

            for (int i = fieldsToUpdate.size() - 1; i >= 0; i--) {
                if (optionalSearch.size() > 1) {
                    String lastInsertion = optionalSearch.get(0);
                    optionalSearch.remove(0);
                    optionalSearch.add(0, Character.toUpperCase(lastInsertion.charAt(0)) + lastInsertion.substring(1));
                }

                optionalSearch.add(0, fieldsToUpdate.get(i));

                String word = String.join("", optionalSearch);
                if (fieldToColumn.containsKey(word)) {
                    fieldsToUpdate.add(word);

                    for (int j = fieldsToUpdate.size() - 1; j >= i; j--)
                        fieldsToUpdate.remove(j);

                    currentWord = new StringBuilder();

                    break;
                }

            }

        }

        if (currentWord.length() > 0) {
            if (!fieldToColumn.containsKey(currentWord.toString()))
                throw new SQLiteException("Field not found: " + currentWord.toString());

            fieldsToUpdate.add(currentWord.toString());
        }

        List<String> columns = new ArrayList<>();

        for (String field : fieldsToUpdate) {
            if (!fieldToColumn.containsKey(field))
                continue;

            columns.add(fieldToColumn.get(field));
        }

        return columns.toArray(new String[0]);
    }

    /**
     * Converts an array of objects to an array of strings for use in SQL queries.
     * Handles various data types and converts them to appropriate string representations.
     *
     * @param args The array of objects to convert
     * @return An array of strings representing the input objects
     * @throws SQLiteException If an unsupported data type is encountered
     */
    public String[] createArgs(Object[] args) {
        if (args == null || args.length == 0)
            return new String[0];

        String[] result = new String[args.length];
        for(int i = 0; i < args.length; i++) {
            String typeName = args[i].getClass().getSimpleName().toLowerCase();

            switch (typeName) {
                case "string":
                case "short":
                case "int":
                case "integer":
                case "long":
                case "double":
                case "float":
                    result[i] = args[i].toString();
                    break;
                case "boolean":
                    boolean value = (boolean) args[i];
                    result[i] = value ? "1" : "0";
                    break;
                case "date":
                    result[i] = String.valueOf(((Date) args[i]).getTime());
                    break;
                default:
                    throw new SQLiteException("Unsupported type for parameter: " + args[i].getClass().getSimpleName());
            }

        }

        return result;
    }

    /**
     * Creates an array of string arguments for SQL queries, starting from a specific index.
     *
     * @param args The original arguments
     * @param startIndex The index to start from
     * @return An array of string arguments
     */
    public String[] createArgs(Object[] args, int startIndex) {
        if (args == null || args.length <= startIndex)
            return new String[0];

        String[] result = new String[args.length - startIndex];
        for(int i = startIndex; i < args.length; i++) {
            String typeName = args[i].getClass().getSimpleName().toLowerCase();

            switch (typeName) {
                case "string":
                case "short":
                case "int":
                case "integer":
                case "long":
                case "double":
                case "float":
                    result[i - startIndex] = args[i].toString();
                    break;
                case "boolean":
                    boolean value = (boolean) args[i];
                    result[i - startIndex] = value ? "1" : "0";
                    break;
                case "date":
                    result[i - startIndex] = String.valueOf(((Date) args[i]).getTime());
                    break;
                default:
                    throw new SQLiteException("Unsupported type for parameter: " + args[i].getClass().getSimpleName());
            }
        }

        return result;
    }

    /**
     * Splits a camelCase string into an array of words.
     * For example, "findByNameAndAge" would be split into ["find", "By", "Name", "And", "Age"].
     *
     * @param input The camelCase string to split
     * @return An array of words
     */
    public String[] splitCamelCase(String input) {
        List<String> words = new ArrayList<>();
        StringBuilder currentWord = new StringBuilder();

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (i > 0 && Character.isUpperCase(c)) {
                words.add(currentWord.toString());
                currentWord = new StringBuilder();
            }

            currentWord.append(c);
        }

        if (currentWord.length() > 0)
            words.add(currentWord.toString());

        return words.toArray(new String[0]);
    }



}
