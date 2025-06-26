package com.jef.sqlite.management.Query;

import com.jef.sqlite.management.SQLiteManagement;

import java.lang.reflect.Proxy;

public class QueryFactory {

    @SuppressWarnings("unchecked")
    public static <T> T create(Class<T> iface, Class<?> entityClass, SQLiteManagement management) {
        return (T) Proxy.newProxyInstance(
                iface.getClassLoader(),
                new Class<?>[]{ iface },
                new QueryInvocationHandler<>(entityClass, management)
        );
    }


}
