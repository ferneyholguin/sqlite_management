package com.jef.sqlite.management.Query.QueryInvocation;

import com.jef.sqlite.management.SQLiteManagement;
import com.jef.sqlite.management.exceptions.SQLiteException;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Manejador de invocación para consultas dinámicas.
 * Implementa InvocationHandler para interceptar llamadas a métodos en interfaces de consulta
 * y proporcionar implementaciones dinámicas basadas en el nombre del metodo.
 * Delega las operaciones a clases especializadas: QueryFindHandler para búsquedas y QuerySaveHandler para guardar.
 *
 * @param <T> El tipo de entidad sobre la que se realizan las consultas
 */
public class QueryInvocationHandler<T> implements InvocationHandler {

    private final Class<T> entityClass;
    private final SQLiteManagement management;
    private final QueryFindHandler<T> findHandler;
    private final QuerySaveHandler<T> saveHandler;

    /**
     * Constructor para QueryInvocationHandler.
     * Inicializa el manejador con la clase de entidad y el gestor de base de datos.
     * Crea instancias de los manejadores especializados para operaciones de búsqueda y guardado.
     *
     * @param entityClass La clase de entidad asociada a la consulta
     * @param management El gestor de la base de datos SQLite
     */
    public QueryInvocationHandler(Class<T> entityClass, SQLiteManagement management) {
        this.entityClass = entityClass;
        this.management = management;
        this.findHandler = new QueryFindHandler<>(entityClass, management);
        this.saveHandler = new QuerySaveHandler<>(entityClass, management);
    }

    /**
     * Metodo principal que intercepta todas las llamadas a métodos en la interfaz de consulta.
     * Analiza el nombre del metodo y los argumentos para determinar qué operación realizar.
     * Distribuye las consultas a los manejadores especializados según el tipo de operación.
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

        // Operaciones de guardado
        if (methodName.equals("save")) {
            if (args == null || args.length == 0 || args[0] == null)
                throw new SQLiteException("Entity is required for save method");

            return saveHandler.save((T) args[0]);
        }

        // Operaciones de búsqueda
        if (methodName.equals("findAll")) {
            return findHandler.findAll();
        } else if (methodName.startsWith("findAllOrderBy")) {
            if (args == null || args.length == 0)
                throw new SQLiteException("Sort direction is required for findAllOrderBy methods");

            String[] argsString = Stream.of(args)
                    .filter(Objects::nonNull)
                    .map(String::valueOf)
                    .toArray(String[]::new);

            if (argsString.length == 0)
                throw new SQLiteException("Sort direction is required for findAllOrderBy methods");

            return findHandler.findAllOrderBy(method, argsString[0]);
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

            return findHandler.findAllByFieldOrderByField(method, argsString);
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
            return findHandler.createFindBy(method, argsString);
        else if (methodName.startsWith("findAllBy") && !methodName.matches("findAllBy\\w+OrderBy\\w+"))
            return findHandler.createFindAllBy(method, argsString);

        throw new UnsupportedOperationException("Method not supported: " + methodName);
    }
}