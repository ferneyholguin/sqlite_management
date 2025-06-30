package com.jef.sqlite.management.Query.QueryInvocation;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import com.jef.sqlite.management.SQLiteManagement;
import com.jef.sqlite.management.exceptions.SQLiteException;
import com.jef.sqlite.management.interfaces.Column;
import com.jef.sqlite.management.interfaces.Table;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
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
                .map(field ->
                        fieldToColumn.get(Character.toLowerCase(field.charAt(0)) + field.substring(1)) + " = ?")
                .collect(Collectors.joining(" AND "));

        String[] whereArgs = Stream.of(args)
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






}