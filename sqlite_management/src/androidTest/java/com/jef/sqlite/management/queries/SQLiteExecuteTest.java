package com.jef.sqlite.management.queries;

import com.jef.sqlite.management.interfaces.DynamicQuery;
import com.jef.sqlite.management.interfaces.SQLiteQuery;
import com.jef.sqlite.management.models.Product;

/**
 * Test interface for demonstrating the SQLiteQuery annotation with waitResult=false.
 * This interface shows how to use the SQLiteQuery annotation to execute non-query SQL operations.
 */
public interface SQLiteExecuteTest extends DynamicQuery<Product> {

    /**
     * Execute a custom SQL statement to update a product's name.
     * This demonstrates using SQLiteQuery with waitResult=false.
     * 
     * @param newName the new name for the product
     * @param id the ID of the product to update
     * @return the number of rows affected (always 1 in this implementation)
     */
    @SQLiteQuery(sql = "UPDATE products SET name = ? WHERE id = ?", captureResult = false)
    int updateProductName(String newName, int id);

    /**
     * Execute a custom SQL statement to delete a product.
     * This demonstrates using SQLiteQuery with waitResult=false.
     * 
     * @param id the ID of the product to delete
     * @return the number of rows affected (always 1 in this implementation)
     */
    @SQLiteQuery(sql = "DELETE FROM products WHERE id = ?", captureResult = false)
    int deleteProduct(int id);

    /**
     * Execute a custom SQL statement to insert a new product.
     * This demonstrates using SQLiteQuery with waitResult=false.
     * 
     * @param name the name of the new product
     * @param lineId the ID of the line for the new product
     * @return the number of rows affected (always 1 in this implementation)
     */
    @SQLiteQuery(sql = "INSERT INTO products (name, line) VALUES (?, ?)", captureResult = false)
    int insertProduct(String name, int lineId);
}