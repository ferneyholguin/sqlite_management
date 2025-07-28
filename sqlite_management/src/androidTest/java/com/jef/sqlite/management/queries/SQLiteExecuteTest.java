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
     * @return true if the update was successful, false otherwise
     */
    @SQLiteQuery(sql = "UPDATE products SET name = ? WHERE id = ?", captureResult = false)
    boolean updateProductName(String newName, int id);

    /**
     * Execute a custom SQL statement to delete a product.
     * This demonstrates using SQLiteQuery with waitResult=false.
     * 
     * @param id the ID of the product to delete
     * @return true if the delete was successful, false otherwise
     */
    @SQLiteQuery(sql = "DELETE FROM products WHERE id = ?", captureResult = false)
    boolean deleteProduct(int id);

    /**
     * Execute a custom SQL statement to insert a new product.
     * This demonstrates using SQLiteQuery with waitResult=false.
     * 
     * @param name the name of the new product
     * @param lineId the ID of the line for the new product
     * @param active whether the product is active
     * @return true if the insert was successful, false otherwise
     */
    @SQLiteQuery(sql = "INSERT INTO products (name, line, active) VALUES (?, ?, ?)", captureResult = false)
    boolean insertProduct(String name, int lineId, boolean active);
}
