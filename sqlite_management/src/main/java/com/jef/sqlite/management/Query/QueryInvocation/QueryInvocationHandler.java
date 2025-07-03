package com.jef.sqlite.management.Query.QueryInvocation;

import android.database.sqlite.SQLiteDatabase;

import com.jef.sqlite.management.SQLiteManagement;
import com.jef.sqlite.management.exceptions.SQLiteException;
import com.jef.sqlite.management.interfaces.SQLiteQuery;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * Manejador de invocación para consultas dinámicas.
 * Implementa InvocationHandler para interceptar llamadas a métodos en interfaces de consulta
 * y proporcionar implementaciones dinámicas basadas en el nombre del metodo.
 * Delega las operaciones a clases especializadas: QueryFindHandler para búsquedas, 
 * QuerySaveHandler para guardar, QueryUpdateHandler para actualizar y QueryExistsHandler para
 * verificar existencia.
 *
 * @param <T> El tipo de entidad sobre la que se realizan las consultas
 */
public class QueryInvocationHandler<T> implements InvocationHandler {

    private final Class<T> entityClass;
    private final SQLiteManagement management;
    private final QueryFindHandler<T> findHandler;
    private final QuerySaveHandler<T> saveHandler;
    private final QueryUpdateHandler<T> updateHandler;
    private final QueryExistsHandler<T> existsHandler;

    /**
     * Constructor para QueryInvocationHandler.
     * Inicializa el manejador con la clase de entidad y el gestor de base de datos.
     * Crea instancias de los manejadores especializados para operaciones de búsqueda, guardado, 
     * actualización y verificación de existencia.
     *
     * @param entityClass La clase de entidad asociada a la consulta
     * @param management El gestor de la base de datos SQLite
     */
    public QueryInvocationHandler(Class<T> entityClass, SQLiteManagement management) {
        this.entityClass = entityClass;
        this.management = management;
        this.findHandler = new QueryFindHandler<>(entityClass, management);
        this.saveHandler = new QuerySaveHandler<>(entityClass, management);
        this.updateHandler = new QueryUpdateHandler<>(entityClass, management);
        this.existsHandler = new QueryExistsHandler<>(entityClass, management);
    }

    /**
     * Metodo principal que intercepta todas las llamadas a métodos en la interfaz de consulta.
     * Analiza el nombre del metodo y los argumentos para determinar qué operación realizar.
     * Distribuye las consultas a los manejadores especializados según el tipo de operación:
     * - Métodos que comienzan con "save" se dirigen a QuerySaveHandler
     * - Métodos que comienzan con "find" se dirigen a QueryFindHandler
     * - Métodos que comienzan con "updateBy" se dirigen a QueryUpdateHandler
     *
     * @param proxy El objeto proxy en el que se invocó el metodo
     * @param method El metodo invocado
     * @param args Los argumentos pasados al metodo
     * @return El resultado de la operación de consulta
     * @throws SQLiteException Si hay errores en los argumentos o en la ejecución de la consulta
     * @throws UnsupportedOperationException Si el metodo invocado no está soportado
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        String methodName = method.getName();

        try {
            // Check for SQLiteQuery annotation
            if (method.isAnnotationPresent(SQLiteQuery.class)) {
                SQLiteQuery sqLiteQuery = method.getAnnotation(SQLiteQuery.class);

                // Validate arguments
                if (args == null) {
                    args = new Object[0];
                }

                if (sqLiteQuery.captureResult())
                    return findHandler.executeCustomQuery(method, args);
                else {
                    return executeCustomQuery(method, args);
                }

            }

            // Operaciones de guardado
            if (methodName.equals("save")) {
                if (args == null || args.length == 0 || args[0] == null)
                    throw new SQLiteException("Entity is required for save method");

                try {
                    return saveHandler.save((T) args[0]);
                } catch (ClassCastException e) {
                    throw new SQLiteException("Entity must be of type " + entityClass.getName());
                }
            }

            // Operaciones de búsqueda
            if (methodName.equals("findAll"))
                return findHandler.findAll();

            if (methodName.matches("findAllOrderBy\\w+(Asc|Desc)?"))
                // Handle methods like findAllOrderByNameAsc or findAllOrderByNameDesc
                return findHandler.findAllOrderBy(method);


            if (args == null || args.length == 0)
                throw new SQLiteException("The args not have null o empty");

            if (methodName.matches("findAllBy\\w+OrderBy\\w+(Asc|Desc)?"))
                return findHandler.findAllByFieldOrderByField(method, args);

            if (methodName.startsWith("findBy"))
                return findHandler.createFindBy(method, args);

            if (methodName.startsWith("findAllBy") && !methodName.matches("findAllBy\\w+OrderBy\\w+"))
                return findHandler.createFindAllBy(method, args);

            if (methodName.startsWith("updateBy"))
                return updateHandler.updateBy(method, args);

            if (methodName.startsWith("existsBy"))
                return existsHandler.handleExistsBy(method, args);

            throw new UnsupportedOperationException("Method not supported: " + methodName);
        } catch (android.database.sqlite.SQLiteException e) {
            // Wrap Android's SQLiteException in our own SQLiteException
            throw new SQLiteException("SQLite error: " + e.getMessage(), e);
        }
    }

    /**
     * Executes a custom SQL query defined in a SQLiteQuery annotation that doesn't return results.
     * This is used for non-query operations like INSERT, UPDATE, DELETE.
     * 
     * @param method The method annotated with SQLiteQuery
     * @param args The arguments passed to the method
     * @return The number of rows affected by the operation
     * @throws SQLiteException If there's an error executing the query
     */
    private boolean executeCustomQuery(Method method, Object[] args) {
        // Get the SQL query from the annotation
        SQLiteQuery annotation = method.getAnnotation(SQLiteQuery.class);
        if (annotation == null)
            throw new SQLiteException("Method is not annotated with SQLiteQuery: " + method.getName());

        String sql = annotation.sql();
        if (sql == null || sql.isEmpty())
            throw new SQLiteException("SQL query cannot be empty in SQLiteQuery annotation");

        // Execute the SQL statement
        SQLiteDatabase db = management.getWritableDatabase();
        try {
            // For non-query operations, we use execSQL
            if (args.length > 0) {
                // Replace ? placeholders with actual values
                for (Object arg : args) {
                    String typeName = arg.getClass().getSimpleName().toLowerCase();

                    switch (typeName) {
                        case "string":
                            sql = sql.replaceFirst("\\?", "'" + arg + "'");
                            break;

                        case "short":
                        case "int":
                        case "integer":
                        case "long":
                        case "double":
                        case "float":
                            sql = sql.replaceFirst("\\?", arg.toString());
                            break;
                        case "boolean":
                            boolean value = (boolean) arg;
                            sql = sql.replaceFirst("\\?", (value ? "1" : "0"));
                            break;

                        default:
                            throw new SQLiteException("Unsupported type for parameter: " + arg.getClass().getSimpleName());
                    }

                }

            }

            db.execSQL(sql);
            return true;

        } catch (android.database.sqlite.SQLiteException e) {
            throw new SQLiteException("Error executing query: " + e.getMessage(), e);
        } finally {
            db.close();
        }
    }


}
