package com.jef.sqlite.management.interfaces;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Anotación para marcar un campo como una columna de una tabla SQLite.
 * Define las propiedades de la columna como nombre, restricciones y valores predeterminados.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Column {

    /**
     * Define el nombre de la columna en la tabla.
     * 
     * @return El nombre de la columna
     */
    String name() default "";

    /**
     * Indica si la columna permite valores nulos.
     * 
     * @return true si la columna permite valores nulos, false en caso contrario
     */
    boolean permitNull() default true;

    /**
     * Indica si la columna es una clave primaria.
     * 
     * @return true si la columna es una clave primaria, false en caso contrario
     */
    boolean primaryKey() default false;

    /**
     * Indica si la columna es de autoincremento.
     * Solo aplicable a columnas de tipo entero que son clave primaria.
     * 
     * @return true si la columna es de autoincremento, false en caso contrario
     */
    boolean autoIncrement() default false;

    /**
     * Indica si la columna tiene una restricción UNIQUE.
     * 
     * @return true si la columna debe tener valores únicos, false en caso contrario
     */
    boolean unique() default false;

    /**
     * Define el valor predeterminado para la columna.
     * 
     * @return El valor predeterminado como cadena
     */
    String defaultValue() default "";

}
