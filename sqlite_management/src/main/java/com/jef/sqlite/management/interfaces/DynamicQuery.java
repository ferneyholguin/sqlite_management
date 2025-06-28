package com.jef.sqlite.management.interfaces;

import java.util.List;

/**
 * Interfaz para consultas dinámicas a la base de datos.
 * Proporciona métodos para buscar, guardar y ordenar entidades.
 *
 * @param <T> El tipo de entidad sobre la que se realizan las consultas
 */
public interface DynamicQuery<T> {

    /**
     * Busca entidades por un valor específico de un campo.
     * 
     * @param fieldName el nombre del campo por el que buscar
     * @param value el valor a buscar
     * @return una lista de entidades que coinciden con el criterio
     */
    List<T> findBy(String fieldName, Object value);

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
     * Busca todas las entidades en la tabla, ordenadas por un campo específico.
     * Patrón de nombre de metodo: findAllOrderBy[NombreCampo]
     * 
     * @param direction la dirección de ordenación ("ASC" o "DESC")
     * @return una lista de todas las entidades en la tabla, ordenadas por el campo especificado
     */
    // Este es un metodo marcador - la implementación real se maneja dinámicamente
    // basado en el patrón de nombre de metodo findAllOrderBy[NombreCampo]

    /**
     * Busca todas las entidades que coinciden con un valor específico de un campo,
     * ordenadas por otro campo.
     * Patrón de nombre de metodo: findAllBy[CampoWhere]OrderBy[CampoOrder]
     * 
     * @param whereValue el valor a buscar en la cláusula WHERE
     * @param direction la dirección de ordenación ("ASC" o "DESC") para la cláusula ORDER BY
     * @return una lista de entidades que coinciden con el criterio y ordenadas por el campo especificado
     */
    // Este es un metodo marcador - la implementación real se maneja dinámicamente
    // basado en el patrón de nombre de metodo findAllBy[CampoWhere]OrderBy[CampoOrder]
}
