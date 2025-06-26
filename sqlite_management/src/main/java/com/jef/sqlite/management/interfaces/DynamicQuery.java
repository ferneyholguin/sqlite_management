package com.jef.sqlite.management.interfaces;

import java.util.List;

public interface DynamicQuery<T> {

    /**
     * Find entities by a specific field value
     * @param fieldName the name of the field to search by
     * @param value the value to search for
     * @return a list of entities matching the criteria
     */
    List<T> findBy(String fieldName, Object value);

    /**
     * Find all entities in the table
     * @return a list of all entities in the table
     */
    List<T> findAll();

    /**
     * Find all entities in the table, ordered by a specific field
     * Method name pattern: findAllOrderBy[FieldName]
     * @param direction the sort direction ("ASC" or "DESC")
     * @return a list of all entities in the table, ordered by the specified field
     */
    // This is a marker method - actual implementation is handled dynamically
    // based on method name pattern findAllOrderBy[FieldName]

    /**
     * Find all entities that match a specific field value, ordered by another field
     * Method name pattern: findAllBy[WhereField]OrderBy[OrderField]
     * @param whereValue the value to search for in the WHERE clause
     * @param direction the sort direction ("ASC" or "DESC") for the ORDER BY clause
     * @return a list of entities matching the criteria and ordered by the specified field
     */
    // This is a marker method - actual implementation is handled dynamically
    // based on method name pattern findAllBy[WhereField]OrderBy[OrderField]
}
