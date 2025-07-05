package com.jef.sqlite.management.queries;

import com.jef.sqlite.management.interfaces.DynamicQuery;
import com.jef.sqlite.management.models.Product;

import java.util.List;
import java.util.Optional;

/**
 * Test interface for QueryFindHandler functionality.
 * This interface includes methods to test all features of QueryFindHandler,
 * including basic queries, dynamic queries with AND/OR conditions, and OrderBy clauses.
 */
public interface QueryFindHandlerTest extends DynamicQuery<Product> {

    /**
     * Basic findAll method.
     * @return all products
     */
    List<Product> findAll();

    /**
     * Find a product by ID.
     * @param id the ID to search for
     * @return the product with the given ID
     */
    Optional<Product> findById(int id);

    /**
     * Find products by name.
     * @param name the name to search for
     * @return list of products with the given name
     */
    List<Product> findByName(String name);

    /**
     * Find products by active status.
     * @param active the active status to search for
     * @return list of products with the given active status
     */
    List<Product> findByActive(boolean active);

    /**
     * Find all products ordered by name ascending.
     * @return all products ordered by name ascending
     */
    List<Product> findAllOrderByNameAsc();

    /**
     * Find all products ordered by name descending.
     * @return all products ordered by name descending
     */
    List<Product> findAllOrderByNameDesc();

    /**
     * Find products by name and active status (AND condition).
     * @param name the name to search for
     * @param active the active status to search for
     * @return list of products with the given name AND active status
     */
    List<Product> findByNameAndActive(String name, boolean active);

    /**
     * Find products by name or active status (OR condition).
     * @param name the name to search for
     * @param active the active status to search for
     * @return list of products with the given name OR active status
     */
    List<Product> findByNameOrActive(String name, boolean active);

    /**
     * Find products by name and active status, ordered by ID ascending.
     * @param name the name to search for
     * @param active the active status to search for
     * @return list of products with the given name and active status, ordered by ID ascending
     */
    List<Product> findByNameAndActiveOrderByIdAsc(String name, boolean active);

    /**
     * Find products by name or active status, ordered by ID descending.
     * @param name the name to search for
     * @param active the active status to search for
     * @return list of products with the given name or active status, ordered by ID descending
     */
    List<Product> findByNameOrActiveOrderByIdDesc(String name, boolean active);

    /**
     * Find all products by name and line ID (multiple AND conditions).
     * @param name the name to search for
     * @param lineId the line ID to search for
     * @return list of products with the given name and line ID
     */
    List<Product> findAllByNameAndLine(String name, int lineId);

    /**
     * Find all products by name or line ID (multiple OR conditions).
     * @param name the name to search for
     * @param lineId the line ID to search for
     * @return list of products with the given name or line ID
     */
    List<Product> findAllByNameOrLine(String name, int lineId);

    /**
     * Find all products by name and active status or line ID (mixed AND/OR conditions).
     * @param name the name to search for
     * @param active the active status to search for
     * @param lineId the line ID to search for
     * @return list of products with the given name and active status, or with the given line ID
     */
    List<Product> findAllByNameAndActiveOrLine(String name, boolean active, int lineId);
}