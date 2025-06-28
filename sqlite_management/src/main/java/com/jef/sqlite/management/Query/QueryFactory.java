package com.jef.sqlite.management.Query;

import com.jef.sqlite.management.Query.QueryInvocation.QueryInvocationHandler;
import com.jef.sqlite.management.SQLiteManagement;

import java.lang.reflect.Proxy;

/**
 * Fábrica para crear instancias de interfaces de consulta dinámicas.
 * Utiliza el mecanismo de proxy dinámico de Java para implementar interfaces en tiempo de ejecución.
 */
public class QueryFactory {

    /**
     * Crea una instancia de una interfaz de consulta dinámica.
     * 
     * @param <T> El tipo de interfaz a implementar
     * @param iface La clase de la interfaz a implementar
     * @param entityClass La clase de entidad asociada a la consulta
     * @param management El gestor de la base de datos SQLite
     * @return Una instancia de la interfaz implementada
     */
    @SuppressWarnings("unchecked")
    public static <T> T create(Class<T> iface, Class<?> entityClass, SQLiteManagement management) {
        return (T) Proxy.newProxyInstance(
                iface.getClassLoader(),
                new Class<?>[]{ iface },
                new QueryInvocationHandler<>(entityClass, management)
        );
    }


}
