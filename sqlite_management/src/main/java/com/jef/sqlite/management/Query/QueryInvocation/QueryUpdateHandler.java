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

        // Map Column fields
        for (Field field : entityClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(Column.class)) {
                fieldToColumn.put(field.getName().toLowerCase(), field.getAnnotation(Column.class).name());
            }
        }

        // Map Join fields
        for (Field field : entityClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(Join.class)) {
                String fieldName = field.getName().toLowerCase();
                String columnName = field.getAnnotation(Join.class).targetName();
                fieldToColumn.put(fieldName, columnName);

                // Add special case for LineId -> line mapping
                if (fieldName.equals("line")) {
                    fieldToColumn.put("lineid", columnName);
                }
            }
        }
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

        // Build WHERE clause with placeholders
        StringBuilder whereClause = new StringBuilder();
        for (int i = 0; i < fieldNames.length; i++) {
            String columnName = fieldToColumn.get(fieldNames[i].toLowerCase());
            if (columnName == null) {
                throw new SQLiteException("Field not found: " + fieldNames[i].toLowerCase());
            }

            if (i > 0) {
                whereClause.append(" AND ");
            }
            whereClause.append(columnName).append(" = ?");
        }

        String[] whereArgs = Stream.of(args)
                .skip(1) // Skip ContentValues
                .map(String::valueOf)
                .toArray(String[]::new);

        // Perform the update operation
        SQLiteDatabase db = management.getWritableDatabase();
        try {
            return db.update(tableName, values, whereClause.toString(), whereArgs);
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

        // Split the method name at "Where" to separate fields to update and conditions
        String[] parts = methodName.split("Where");

        if (parts.length == 1) {
            // No "Where" in the method name, just update fields
            String fieldsToUpdatePart = parts[0].substring("update".length());
            return processUpdate(method, args, fieldsToUpdatePart, null);
        } else if (parts.length == 2) {
            // Has "Where" in the method name
            String fieldsToUpdatePart = parts[0].substring("update".length());
            String whereConditionsPart = parts[1];

            System.out.println("[DEBUG_LOG] Method name: " + methodName);
            System.out.println("[DEBUG_LOG] Fields to update: " + fieldsToUpdatePart);
            System.out.println("[DEBUG_LOG] Where conditions: " + whereConditionsPart);

            return processUpdate(method, args, fieldsToUpdatePart, whereConditionsPart);
        } else {
            throw new SQLiteException("Method name has multiple 'Where' keywords: " + methodName);
        }
    }

    /**
     * Procesa una actualización basada en partes del nombre del metodo y argumentos.
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
        if (args.length != (fieldsToUpdate.length + whereConditions.length))
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
