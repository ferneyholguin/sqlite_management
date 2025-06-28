package com.jef.sqlite.management.interfaces;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Anotación para marcar una clase como una tabla de base de datos SQLite.
 * Se utiliza para definir el nombre de la tabla en la base de datos.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Table {

    /**
     * Define el nombre de la tabla en la base de datos.
     * 
     * @return El nombre de la tabla
     */
    String name() default "";

}
