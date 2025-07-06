package com.jef.sqlite.management.tests;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.jef.sqlite.management.Management;
import com.jef.sqlite.management.Query.QueryFactory;
import com.jef.sqlite.management.models.Line;
import com.jef.sqlite.management.models.Product;
import com.jef.sqlite.management.queries.SQLiteExecuteTest;
import com.jef.sqlite.management.tables.LineTable;
import com.jef.sqlite.management.tables.TableProducts;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;

/**
 * Test class that demonstrates how to use the SQLiteQuery annotation with waitResult=false.
 * This test shows how to execute non-query SQL operations using the SQLiteQuery annotation.
 */
@RunWith(AndroidJUnit4.class)
public class SQLiteExecuteTestDemo {

    private Context context;
    private TableProducts productTable;
    private LineTable lineTable;
    private SQLiteExecuteTest sqliteExecuteTest;

    @Before
    public void setup() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        // Clear existing data
        context.deleteDatabase("test.db");

        // Initialize tables
        productTable = new TableProducts(context);
        lineTable = new LineTable(context);

        // Create the SQLiteExecuteTest interface implementation
        Management management = new Management(context);
        sqliteExecuteTest = QueryFactory.create(SQLiteExecuteTest.class, management);

        // Setup test data
        setupTestData();
    }

    private void setupTestData() {
        // Create lines
        Line line1 = new Line();
        line1.setName("Line 1");
        line1 = lineTable.saveLine(line1);

        Line line2 = new Line();
        line2.setName("Line 2");
        line2 = lineTable.saveLine(line2);

        // Create products
        Product product1 = new Product();
        product1.setName("Product 1");
        product1.setLine(line1);
        productTable.saveProduct(product1);

        Product product2 = new Product();
        product2.setName("Product 2");
        product2.setLine(line1);
        productTable.saveProduct(product2);

        Product product3 = new Product();
        product3.setName("Product 3");
        product3.setLine(line2);
        productTable.saveProduct(product3);
    }

    @Test
    public void testUpdateProductName() {
        // Get a product to update
        Optional<Product> productOpt = productTable.getProductById(1);
        assertTrue("Product with ID 1 should exist", productOpt.isPresent());

        Product product = productOpt.get();
        assertEquals("Product 1", product.getName());

        // Update the product name using the custom SQL query
        int rowsAffected = sqliteExecuteTest.updateProductName("Updated Product 1", 1);

        // Verify the update was successful
        assertEquals("Should affect 1 row", 1, rowsAffected);

        // Verify the product was updated
        productOpt = productTable.getProductById(1);
        assertTrue("Product with ID 1 should still exist", productOpt.isPresent());

        product = productOpt.get();
        assertEquals("Updated Product 1", product.getName());
    }

    @Test
    public void testDeleteProduct() {
        // Verify product with ID 2 exists
        Optional<Product> productOpt = productTable.getProductById(2);
        assertTrue("Product with ID 2 should exist", productOpt.isPresent());

        // Delete the product using the custom SQL query
        int rowsAffected = sqliteExecuteTest.deleteProduct(2);

        // Verify the delete was successful
        assertEquals("Should affect 1 row", 1, rowsAffected);

        // Verify the product was deleted
        productOpt = productTable.getProductById(2);
        assertFalse("Product with ID 2 should no longer exist", productOpt.isPresent());
    }

    @Test
    public void testInsertProduct() {
        // Get the current number of products
        List<Product> products = productTable.getAllProducts();
        int initialCount = products.size();

        // Insert a new product using the custom SQL query
        int rowsAffected = sqliteExecuteTest.insertProduct("New Product", 1);

        // Verify the insert was successful
        assertEquals("Should affect 1 row", 1, rowsAffected);

        // Verify the product was inserted
        products = productTable.getAllProducts();
        assertEquals("Should have one more product", initialCount + 1, products.size());

        // Verify the new product has the correct name
        boolean foundNewProduct = false;
        for (Product product : products) {
            if ("New Product".equals(product.getName())) {
                foundNewProduct = true;
                break;
            }
        }
        assertTrue("Should find the new product", foundNewProduct);
    }
}
