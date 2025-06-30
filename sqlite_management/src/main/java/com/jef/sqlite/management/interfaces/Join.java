package com.jef.sqlite.management.interfaces;


import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Anotación para marcar un campo como una relación entre tablas.
 * Define las propiedades de la relación como la tabla destino, la columna fuente y restricciones.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Join {

    /**
     * Define el nombre de la columna en la tabla actual que almacena la referencia.
     * 
     * @return El nombre de la columna de referencia
     */
    String targetName();

    /**
     * Define la clase de entidad relacionada.
     * 
     * @return La clase de la entidad relacionada
     */
    Class<?> relationShip();

    /**
     * Define el nombre de la variable en la entidad relacionada que almacena la referencia.
     * 
     * @return El nombre de la columna fuente en la tabla relacionada
     */
    String source();

    /**
     * Define el valor predeterminado para la relación.
     * 
     * @return El valor predeterminado como cadena
     */
    String defaultValue() default "";

    /**
     * Indica si la relación debe ser única.
     * 
     * @return true si la relación debe ser única, false en caso contrario
     */
    boolean isUnique() default false;

    /**
     * Indica si la relación permite valores nulos.
     * 
     * @return true si la relación permite valores nulos, false en caso contrario
     */
    boolean permitNull() default false;

}
