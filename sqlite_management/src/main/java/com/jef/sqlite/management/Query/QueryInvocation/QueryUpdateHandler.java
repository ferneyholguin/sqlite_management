package com.jef.sqlite.management.Query.QueryInvocation;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import com.jef.sqlite.management.SQLiteManagement;
import com.jef.sqlite.management.exceptions.SQLiteException;
import com.jef.sqlite.management.interfaces.Column;
import com.jef.sqlite.management.interfaces.Table;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Manejador para operaciones de actualización en la base de datos.
 * Proporciona métodos para actualizar entidades basados en diferentes criterios.
 *
 * @param <T> El tipo de entidad sobre la que se realizan las actualizaciones
 */
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
        this.tableName = entityClass.getAnnotation(Table.class).name();
        this.fieldToColumn = new HashMap<>();

        for (Field field : entityClass.getDeclaredFields())
            if (field.isAnnotationPresent(Column.class))
                fieldToColumn.put(field.getName().toLowerCase(), field.getAnnotation(Column.class).name());
    }

    /**
     * Actualiza entidades en la base de datos basado en criterios específicos.
     * Analiza el nombre del metodo para determinar los campos por los que filtrar.
     *
     * @param method El metodo invocado
     * @param args Los argumentos pasados al metodo (ContentValues y valores para los criterios where)
     * @return El número de filas actualizadas
     * @throws SQLiteException Si hay errores en los argumentos o en la ejecución de la actualización
     */
    public int updateBy(Method method, Object[] args) {
        if (args == null || args.length < 2 || !(args[0] instanceof ContentValues))
            throw new SQLiteException("ContentValues and at least one where clause value are required");

        ContentValues values = (ContentValues) args[0];
        if (values.size() == 0)
            throw new SQLiteException("ContentValues cannot be empty");

        String methodName = method.getName();
        if (!methodName.startsWith("updateBy"))
            throw new SQLiteException("Method name must start with 'updateBy'");

        // Extract field names from method name
        String fieldsString = methodName.substring("updateBy".length());
        String[] fieldNames = fieldsString.split("And");

        if (fieldNames.length != args.length - 1)
            throw new SQLiteException("Number of field names in method (" + fieldNames.length + 
                                     ") does not match number of where clause values (" + (args.length - 1) + ")");


        String whereClause = Stream.of(fieldNames)
                .map(field -> fieldToColumn.get(field.toLowerCase()))
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" AND "));

        String[] whereArgs = Stream.of(args)
                .skip(1) // Skip ContentValues
                .map(String::valueOf)
                .toArray(String[]::new);

        // Perform the update operation
        SQLiteDatabase db = management.getWritableDatabase();
        try {
            return db.update(tableName, values, whereClause, whereArgs);
        } catch (android.database.sqlite.SQLiteException e) {
            throw new SQLiteException("Error updating entity: " + e.getMessage(), e);
        }
    }

    /**
     * Maneja métodos de actualización con formato "update[Fields]Where[Conditions]".
     * Permite actualizar campos específicos basados en condiciones WHERE sin necesidad de ContentValues.
     *
     * @param method El metodo invocado
     * @param args Los argumentos pasados al metodo (valores para actualizar y valores para los criterios where)
     * @return El número de filas actualizadas
     * @throws SQLiteException Si hay errores en los argumentos o en la ejecución de la actualización
     */
    public int update(Method method, Object[] args) {
        if (args == null || args.length == 0)
            throw new SQLiteException("At least one argument is required");

        String methodName = method.getName();
        if (!methodName.startsWith("update"))
            throw new SQLiteException("Method name must start with 'update'");

        // Check if the method has a "Where" part
        Pattern pattern = Pattern.compile("update([A-Za-z0-9]+)(?:Where([A-Za-z0-9]+))?");
        Matcher matcher = pattern.matcher(methodName);

        if (!matcher.matches()) {
            // Try with a more complex pattern that handles "And" in the WHERE conditions
            pattern = Pattern.compile("update([A-Za-z0-9]+)Where([A-Za-z0-9]+)And([A-Za-z0-9]+)");
            matcher = pattern.matcher(methodName);

            if (!matcher.matches()) {
                // Debug output
                System.out.println("[DEBUG_LOG] Method name: " + methodName);

                // Try with a more flexible pattern that can handle any combination of fields and conditions
                pattern = Pattern.compile("update([A-Za-z0-9]+)Where(.+)");
                matcher = pattern.matcher(methodName);

                if (!matcher.matches())
                    throw new SQLiteException("Method name must follow pattern 'update[Fields]Where[Conditions]'");

                String fieldsToUpdatePart = matcher.group(1);
                String whereConditionsPart = matcher.group(2);
                System.out.println("[DEBUG_LOG] Fields to update: " + fieldsToUpdatePart);
                System.out.println("[DEBUG_LOG] Where conditions: " + whereConditionsPart);
                return processUpdate(method, args, fieldsToUpdatePart, whereConditionsPart);
            }

            String fieldsToUpdatePart = matcher.group(1);
            String whereConditionsPart = matcher.group(2) + "And" + matcher.group(3);
            return processUpdate(method, args, fieldsToUpdatePart, whereConditionsPart);
        }

        String fieldsToUpdatePart = matcher.group(1);
        String whereConditionsPart = matcher.group(2);

        return processUpdate(method, args, fieldsToUpdatePart, whereConditionsPart);
    }

    /**
     * Procesa una actualización basada en partes del nombre del método y argumentos.
     * 
     * @param method El metodo invocado
     * @param args Los argumentos pasados al metodo
     * @param fieldsToUpdatePart La parte del nombre del metodo que indica los campos a actualizar
     * @param whereConditionsPart La parte del nombre del metodo que indica las condiciones WHERE
     * @return El número de filas actualizadas
     * @throws SQLiteException Si hay errores en los argumentos o en la ejecución de la actualización
     */
    private int processUpdate(Method method, Object[] args, String fieldsToUpdatePart, String whereConditionsPart) {
        // Parse fields to update
        String[] fieldsToUpdate = splitCamelCase(fieldsToUpdatePart);

        // Create ContentValues with the fields to update
        ContentValues values = new ContentValues();
        for (int i = 0; i < fieldsToUpdate.length; i++) {
            String fieldName = fieldsToUpdate[i].toLowerCase();
            String columnName = fieldToColumn.get(fieldName);
            if (columnName == null)
                throw new SQLiteException("Field not found: " + fieldName);

            values.put(columnName, args[i].toString());
        }

        // If there are no WHERE conditions, update all rows
        if (whereConditionsPart == null || whereConditionsPart.isEmpty()) {
            SQLiteDatabase db = management.getWritableDatabase();
            try {
                return db.update(tableName, values, null, null);
            } catch (android.database.sqlite.SQLiteException e) {
                throw new SQLiteException("Error updating entity: " + e.getMessage(), e);
            }
        }

        // Parse WHERE conditions
        String[] whereConditions = splitCamelCase(whereConditionsPart);

        // Check if we have enough arguments
        if (args.length != fieldsToUpdate.length + whereConditions.length)
            throw new SQLiteException("Number of arguments (" + args.length + 
                                     ") does not match number of fields to update (" + fieldsToUpdate.length + 
                                     ") plus number of WHERE conditions (" + whereConditions.length + ")");

        // Extract WHERE clause
        String whereClause = Stream.of(whereConditions)
                .map(w -> {
                    String fieldName = w.toLowerCase();
                    String columnName = fieldToColumn.get(fieldName);
                    if (columnName == null) {
                        throw new SQLiteException("Field not found: " + fieldName);
                    }
                    return columnName + " = ?";
                })
                .collect(Collectors.joining(" AND "));

        // Extract WHERE args - skip the update field values
        String[] whereArgs = new String[whereConditions.length];
        for (int i = 0; i < whereConditions.length; i++) {
            whereArgs[i] = String.valueOf(args[i + fieldsToUpdate.length]);
        }

        // Perform the update operation
        SQLiteDatabase db = management.getWritableDatabase();
        try {
            return db.update(tableName, values, whereClause, whereArgs);
        } catch (android.database.sqlite.SQLiteException e) {
            throw new SQLiteException("Error updating entity: " + e.getMessage(), e);
        }
    }

    /**
     * Divide una cadena en formato camelCase en palabras individuales.
     * Por ejemplo, "nameLastNameId" se convierte en ["name", "LastName", "Id"].
     *
     * @param input La cadena en formato camelCase
     * @return Un array de palabras individuales
     */
    private String[] splitCamelCase(String input) {
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