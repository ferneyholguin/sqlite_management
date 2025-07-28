package com.jef.sqlite.management.interfaces;

import com.jef.sqlite.management.exceptions.SQLiteException;

import java.util.List;

/**
 * Interfaz para consultas dinámicas a la base de datos.
 * Proporciona métodos para buscar, guardar y ordenar entidades.
 *
 * @param <T> El tipo de entidad sobre la que se realizan las consultas
 */
public interface DynamicQuery<T> {


    /**
     * Busca todas las entidades en la tabla.
     * 
     * @return una lista de todas las entidades en la tabla
     */
    List<T> findAll();

    /**
     * Guarda una entidad en la base de datos. Si la entidad tiene un valor de clave primaria
     * y existe en la base de datos, se actualizará. De lo contrario, se insertará.
     * 
     * @param entity La entidad a guardar
     * @return La entidad guardada con cualquier valor autogenerado (como IDs de autoincremento)
     */
    T save(T entity);


    /**
     * Valida una entidad para asegurar que cumple con todas las restricciones de la base de datos.
     * Verifica que:
     * 1. Los campos marcados como no nulos tengan valores
     * 2. Los campos marcados como únicos no dupliquen valores existentes
     * 3. Las relaciones requeridas (Join) estén presentes
     * 
     * @param entity La entidad a validar
     * @return true si la entidad es válida, false en caso contrario
     */
    boolean validate(T entity);

    /**
     * Valida una entidad para asegurar que cumple con todas las restricciones de la base de datos.
     * Verifica que:
     * 1. Los campos marcados como no nulos tengan valores
     * 2. Los campos marcados como únicos no dupliquen valores existentes
     * 3. Las relaciones requeridas (Join) estén presentes
     * 
     * A diferencia del método validate(T entity), este método arroja una excepción
     * si la entidad no es válida, con detalles específicos sobre los errores de validación.
     * 
     * @param entity La entidad a validar
     * @throws SQLiteException Si hay errores de validación con detalles específicos
     */
    void validateOrThrow(T entity) throws SQLiteException;

}
