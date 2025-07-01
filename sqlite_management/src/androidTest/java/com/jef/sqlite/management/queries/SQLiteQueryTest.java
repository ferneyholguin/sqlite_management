package com.jef.sqlite.management.queries;

import com.jef.sqlite.management.interfaces.DynamicQuery;
import com.jef.sqlite.management.interfaces.SQLiteQuery;
import com.jef.sqlite.management.models.Product;

import java.util.List;
import java.util.Optional;

/**
 * Test interface for demonstrating the SQLiteQuery annotation.
 * This interface shows how to use the SQLiteQuery annotation to define custom SQL queries.
 */
public interface SQLiteQueryTest extends DynamicQuery<Product> {

    /**
     * Find products with a specific name using a custom SQL query.
     * This demonstrates using SQLiteQuery with a List return type.
     * 
     * @param name the name to search for
     * @return a list of products with the given name
     */
    @SQLiteQuery(sql = "SELECT * FROM products WHERE name = ?")
    List<Product> findProductsByName(String name);

    /**
     * Find a product by its ID using a custom SQL query.
     * This demonstrates using SQLiteQuery with an Optional return type.
     * 
     * @param id the ID to search for
     * @return an Optional containing the product if found, or empty if not found
     */
    @SQLiteQuery(sql = "SELECT * FROM products WHERE id = ?")
    Optional<Product> findProductById(int id);

    /**
     * Find all products in a specific line using a custom SQL query.
     * This demonstrates using SQLiteQuery with a List return type and a JOIN.
     * 
     * @param lineId the line ID to search for
     * @return a list of products in the given line
     */
    @SQLiteQuery(sql = "SELECT p.* FROM products p JOIN lines l ON p.line = l.id WHERE l.id = ?")
    List<Product> findProductsInLine(int lineId);

    /**
     * Find all products with IDs in a specific list using a custom SQL query.
     * This demonstrates using SQLiteQuery with a List return type and an IN clause.
     * 
     * @param ids a comma-separated list of IDs to search for
     * @return a list of products with the given IDs
     */
    @SQLiteQuery(sql = "SELECT * FROM products WHERE id IN (?)")
    List<Product> findProductsWithIdsIn(String ids);

    /**
     * Find all active products using a custom SQL query.
     * This demonstrates using SQLiteQuery to filter by a boolean field.
     * 
     * @return a list of active products
     */
    @SQLiteQuery(sql = "SELECT * FROM products WHERE active = 1")
    List<Product> findActiveProducts();

    /**
     * Find all inactive products using a custom SQL query.
     * This demonstrates using SQLiteQuery to filter by a boolean field.
     * 
     * @return a list of inactive products
     */
    @SQLiteQuery(sql = "SELECT * FROM products WHERE active = 0")
    List<Product> findInactiveProducts();
}
