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

    // Store product information for tests
    private Product savedProduct1;
    private Product savedProduct2;
    private Product savedProduct3;

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
        try {
            System.out.println("[DEBUG_LOG] Starting setupTestData");

            // Generate a unique timestamp to ensure unique product names
            long timestamp = System.currentTimeMillis();

            // Create lines
            System.out.println("[DEBUG_LOG] Creating Line 1");
            Line line1 = new Line();
            line1.setName("Line 1_" + timestamp);
            line1 = lineTable.saveLine(line1);
            System.out.println("[DEBUG_LOG] Saved Line 1 with ID: " + line1.getId());

            System.out.println("[DEBUG_LOG] Creating Line 2");
            Line line2 = new Line();
            line2.setName("Line 2_" + timestamp);
            line2 = lineTable.saveLine(line2);
            System.out.println("[DEBUG_LOG] Saved Line 2 with ID: " + line2.getId());

            // Create products with unique names using timestamp
            System.out.println("[DEBUG_LOG] Creating Product 1");
            Product product1 = new Product();
            product1.setName("Product 1_" + timestamp);
            product1.setLine(line1);
            product1.setActive(true);
            this.savedProduct1 = productTable.saveProduct(product1);
            System.out.println("[DEBUG_LOG] Saved Product 1 with ID: " + this.savedProduct1.getId());

            System.out.println("[DEBUG_LOG] Creating Product 2");
            Product product2 = new Product();
            product2.setName("Product 2_" + timestamp);
            product2.setLine(line1);
            product2.setActive(true);
            this.savedProduct2 = productTable.saveProduct(product2);
            System.out.println("[DEBUG_LOG] Saved Product 2 with ID: " + this.savedProduct2.getId());

            System.out.println("[DEBUG_LOG] Creating Product 3");
            Product product3 = new Product();
            product3.setName("Product 3_" + timestamp);
            product3.setLine(line2);
            product3.setActive(false);
            this.savedProduct3 = productTable.saveProduct(product3);
            System.out.println("[DEBUG_LOG] Saved Product 3 with ID: " + this.savedProduct3.getId());

            System.out.println("[DEBUG_LOG] setupTestData complete");
        } catch (Exception e) {
            System.out.println("[DEBUG_LOG] Exception in setupTestData: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Test
    public void testUpdateProductName() {
        try {
            System.out.println("[DEBUG_LOG] Starting testUpdateProductName");

            // Get the product ID to update
            int productId = savedProduct1.getId();
            String originalName = savedProduct1.getName();
            String newName = "Updated " + originalName;

            System.out.println("[DEBUG_LOG] Getting product with ID: " + productId);
            // Get a product to update
            Optional<Product> productOpt = productTable.getProductById(productId);
            assertTrue("Product with ID " + productId + " should exist", productOpt.isPresent());

            Product product = productOpt.get();
            assertEquals(originalName, product.getName());
            System.out.println("[DEBUG_LOG] Original product name: " + product.getName());

            // Update the product name using the custom SQL query
            System.out.println("[DEBUG_LOG] Updating product name to: " + newName);
            boolean success = sqliteExecuteTest.updateProductName(newName, productId);
            System.out.println("[DEBUG_LOG] Update successful: " + success);

            // Verify the update was successful
            assertTrue("Update should be successful", success);

            // Verify the product was updated
            System.out.println("[DEBUG_LOG] Getting updated product");
            productOpt = productTable.getProductById(productId);
            assertTrue("Product with ID " + productId + " should still exist", productOpt.isPresent());

            product = productOpt.get();
            System.out.println("[DEBUG_LOG] Updated product name: " + product.getName());
            assertEquals(newName, product.getName());

            System.out.println("[DEBUG_LOG] testUpdateProductName passed");
        } catch (Exception e) {
            System.out.println("[DEBUG_LOG] Exception in testUpdateProductName: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Test
    public void testDeleteProduct() {
        try {
            System.out.println("[DEBUG_LOG] Starting testDeleteProduct");

            // Get the product ID to delete
            int productId = savedProduct2.getId();

            System.out.println("[DEBUG_LOG] Getting product with ID: " + productId);
            // Verify product exists
            Optional<Product> productOpt = productTable.getProductById(productId);
            assertTrue("Product with ID " + productId + " should exist", productOpt.isPresent());
            System.out.println("[DEBUG_LOG] Product exists with name: " + productOpt.get().getName());

            // Delete the product using the custom SQL query
            System.out.println("[DEBUG_LOG] Deleting product with ID: " + productId);
            boolean success = sqliteExecuteTest.deleteProduct(productId);
            System.out.println("[DEBUG_LOG] Delete successful: " + success);

            // Verify the delete was successful
            assertTrue("Delete should be successful", success);

            // Verify the product was deleted
            System.out.println("[DEBUG_LOG] Checking if product was deleted");
            productOpt = productTable.getProductById(productId);
            assertFalse("Product with ID " + productId + " should no longer exist", productOpt.isPresent());
            System.out.println("[DEBUG_LOG] Product was successfully deleted");

            System.out.println("[DEBUG_LOG] testDeleteProduct passed");
        } catch (Exception e) {
            System.out.println("[DEBUG_LOG] Exception in testDeleteProduct: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Test
    public void testInsertProduct() {
        try {
            System.out.println("[DEBUG_LOG] Starting testInsertProduct");

            // Get the current number of products
            List<Product> products = productTable.getAllProducts();
            int initialCount = products.size();
            System.out.println("[DEBUG_LOG] Initial product count: " + initialCount);

            // Get the line ID to use for the new product
            int lineId = savedProduct1.getLine().getId();
            System.out.println("[DEBUG_LOG] Using line ID: " + lineId);

            // Create a unique product name with timestamp
            String newProductName = "New Product_" + System.currentTimeMillis();
            System.out.println("[DEBUG_LOG] New product name: " + newProductName);

            // Insert a new product using the custom SQL query
            System.out.println("[DEBUG_LOG] Inserting new product");
            boolean success = sqliteExecuteTest.insertProduct(newProductName, lineId, true);
            System.out.println("[DEBUG_LOG] Insert successful: " + success);

            // Verify the insert was successful
            assertTrue("Insert should be successful", success);

            // Verify the product was inserted
            System.out.println("[DEBUG_LOG] Getting updated product list");
            products = productTable.getAllProducts();
            System.out.println("[DEBUG_LOG] New product count: " + products.size());
            assertEquals("Should have one more product", initialCount + 1, products.size());

            // Verify the new product has the correct name
            System.out.println("[DEBUG_LOG] Checking if new product exists");
            boolean foundNewProduct = false;
            for (Product product : products) {
                System.out.println("[DEBUG_LOG] Checking product: " + product.getName());
                if (newProductName.equals(product.getName())) {
                    foundNewProduct = true;
                    System.out.println("[DEBUG_LOG] Found new product: " + product.getName());
                    break;
                }
            }
            assertTrue("Should find the new product", foundNewProduct);

            System.out.println("[DEBUG_LOG] testInsertProduct passed");
        } catch (Exception e) {
            System.out.println("[DEBUG_LOG] Exception in testInsertProduct: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
}
