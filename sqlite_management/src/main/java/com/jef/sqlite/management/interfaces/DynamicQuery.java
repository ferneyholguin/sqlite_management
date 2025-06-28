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
     * Patrón de nombre de metodo: findAllOrderBy[NombreCampo][Asc|Desc]
     * 
     * Ejemplos:
     * - findAllOrderByNameAsc() - ordena por nombre en orden ascendente
     * - findAllOrderByNameDesc() - ordena por nombre en orden descendente
     * - findAllOrderByDateCreatedDesc() - ordena por fecha de creación en orden descendente
     * 
     * @return una lista de todas las entidades en la tabla, ordenadas por el campo especificado
     */
    // Este es un metodo marcador - la implementación real se maneja dinámicamente
    // basado en el patrón de nombre de metodo findAllOrderBy[NombreCampo][Asc|Desc]

    /**
     * Busca todas las entidades que coinciden con un valor específico de un campo,
     * ordenadas por otro campo.
     * Patrón de nombre de metodo: findAllBy[CampoWhere]OrderBy[CampoOrder][Asc|Desc]
     * 
     * Ejemplos:
     * - findAllByNameOrderByIdAsc(String name) - busca por nombre y ordena por ID en orden ascendente
     * - findAllByStatusOrderByDateDesc(String status) - busca por estado y ordena por fecha en orden descendente
     * - findAllByCategoryOrderByPriceAsc(String category) - busca por categoría y ordena por precio en orden ascendente
     * 
     * @param whereValue el valor a buscar en la cláusula WHERE
     * @return una lista de entidades que coinciden con el criterio y ordenadas por el campo especificado
     */
    // Este es un metodo marcador - la implementación real se maneja dinámicamente
    // basado en el patrón de nombre de metodo findAllBy[CampoWhere]OrderBy[CampoOrder][Asc|Desc]

    /**
     * Actualiza un campo específico de las entidades que coinciden con múltiples condiciones.
     * Patrón de nombre de metodo: update[Campo]Where[Condicion1]And[Condicion2]...
     * 
     * Ejemplos:
     * - updateStateWhereLocationAndCountry(boolean state, String location, String country)
     * - updateValueWhereSourceAndTagAndControler(int value, String source, String tag, String controller)
     * 
     * @param updateValue el valor a establecer en el campo a actualizar
     * @param whereValues los valores para las condiciones WHERE
     * @return el número de filas afectadas
     */
    // Este es un metodo marcador - la implementación real se maneja dinámicamente
    // basado en el patrón de nombre de metodo update[Campo]Where[Condicion1]And[Condicion2]...
}
