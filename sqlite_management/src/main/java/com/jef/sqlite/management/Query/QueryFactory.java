package com.jef.sqlite.management.Query;

import com.jef.sqlite.management.Query.QueryInvocation.QueryInvocationHandler;
import com.jef.sqlite.management.SQLiteManagement;
import com.jef.sqlite.management.interfaces.DynamicQuery;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;

/**
 * Fábrica para crear instancias de interfaces de consulta dinámicas.
 * Utiliza el mecanismo de proxy dinámico de Java para implementar interfaces en tiempo de ejecución.
 */
public class QueryFactory {

    /**
     * Crea una instancia de una interfaz de consulta dinámica.
     * Extrae automáticamente la clase de entidad desde la interfaz.
     * 
     * @param <T> El tipo de interfaz a implementar
     * @param iface La clase de la interfaz a implementar
     * @param management El gestor de la base de datos SQLite
     * @return Una instancia de la interfaz implementada
     */
    @SuppressWarnings("unchecked")
    public static <T> T create(Class<T> iface, SQLiteManagement management) {
        Class<?> entityClass = extractEntityClassFromInterface(iface);
        return (T) Proxy.newProxyInstance(
                iface.getClassLoader(),
                new Class<?>[]{ iface },
                new QueryInvocationHandler<>(entityClass, management)
        );
    }

    /**
     * Extrae la clase de entidad desde la interfaz de consulta.
     * Busca la interfaz DynamicQuery en la jerarquía de interfaces y extrae su parámetro de tipo.
     * 
     * @param iface La clase de la interfaz a analizar
     * @return La clase de entidad asociada a la interfaz
     * @throws IllegalArgumentException Si no se puede determinar la clase de entidad
     */
    private static Class<?> extractEntityClassFromInterface(Class<?> iface) {
        for (Type genericInterface : iface.getGenericInterfaces()) {
            if (genericInterface instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) genericInterface;
                Class<?> rawType = (Class<?>) parameterizedType.getRawType();

                if (DynamicQuery.class.isAssignableFrom(rawType)) {
                    Type[] typeArguments = parameterizedType.getActualTypeArguments();
                    if (typeArguments.length > 0) {
                        return (Class<?>) typeArguments[0];
                    }
                }
            }
        }

        // Si no se encuentra directamente, buscar en las interfaces padre
        for (Class<?> parentInterface : iface.getInterfaces()) {
            if (DynamicQuery.class.isAssignableFrom(parentInterface)) {
                try {
                    return extractEntityClassFromInterface(parentInterface);
                } catch (IllegalArgumentException e) {
                    // Continuar con la siguiente interfaz si esta no tiene el tipo
                }
            }
        }

        throw new IllegalArgumentException("No se pudo determinar la clase de entidad para la interfaz " + iface.getName());
    }
}
