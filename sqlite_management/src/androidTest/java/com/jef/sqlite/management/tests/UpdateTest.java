package com.jef.sqlite.management.tests;

import android.content.ContentValues;
import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.jef.sqlite.management.Management;
import com.jef.sqlite.management.SQLiteTable;
import com.jef.sqlite.management.models.Product;
import com.jef.sqlite.management.models.Line;
import com.jef.sqlite.management.queries.ProductQuery;
import com.jef.sqlite.management.tables.TableProducts;
import com.jef.sqlite.management.tables.LineTable;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class UpdateTest {

    private Context context;
    private TableProducts productTable;
    private LineTable lineTable;

    @Before
    public void setup() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        // Close any existing database connections
        if (productTable != null) {
            productTable.getManagement().close();
        }
        if (lineTable != null) {
            lineTable.getManagement().close();
        }

        // Clear existing data - make sure the database is completely deleted
        // The database name is "management" as defined in the Management class
        boolean deleted = context.deleteDatabase("management");
        System.out.println("[DEBUG_LOG] Database deleted: " + deleted);

        // Wait a moment to ensure the database is fully deleted
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Initialize tables with a new database connection
        productTable = new TableProducts(context);
        lineTable = new LineTable(context);

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
    public void testUpdateById() {
        // Get a product to update
        Optional<Product> productOpt = productTable.getProductById(1);
        assertTrue("Product with ID 1 should exist", productOpt.isPresent());

        Product product = productOpt.get();
        assertEquals("Product 1", product.getName());

        // Create ContentValues with updated data
        ContentValues values = new ContentValues();
        values.put("name", "Updated Product 1");

        // Update the product
        int rowsUpdated = productTable.updateProductById(values, 1);

        // Verify update was successful
        assertEquals("Should update 1 row", 1, rowsUpdated);

        // Verify the product was updated
        productOpt = productTable.getProductById(1);
        assertTrue("Product with ID 1 should still exist", productOpt.isPresent());

        product = productOpt.get();
        assertEquals("Updated Product 1", product.getName());
    }

    @Test
    public void testUpdateByName() {
        // Get products with name "Product 2"
        List<Product> products = productTable.getProductsByName("Product 2");
        assertFalse("Products with name 'Product 2' should exist", products.isEmpty());
        assertEquals("Should be 1 product with name 'Product 2'", 1, products.size());

        // Create ContentValues with updated data
        ContentValues values = new ContentValues();
        values.put("name", "Updated Product 2");

        // Update the products
        int rowsUpdated = productTable.updateProductByName(values, "Product 2");

        // Verify update was successful
        assertEquals("Should update 1 row", 1, rowsUpdated);

        // Verify the products were updated
        products = productTable.getProductsByName("Updated Product 2");
        assertFalse("Products with name 'Updated Product 2' should exist", products.isEmpty());
        assertEquals("Should be 1 product with name 'Updated Product 2'", 1, products.size());

        // Original name should no longer exist
        products = productTable.getProductsByName("Product 2");
        assertTrue("Products with name 'Product 2' should no longer exist", products.isEmpty());
    }

    @Test
    public void testUpdateNameWhereId() {
        // Get a product to update
        Optional<Product> productOpt = productTable.getProductById(1);
        assertTrue("Product with ID 1 should exist", productOpt.isPresent());

        Product product = productOpt.get();
        assertEquals("Product 1", product.getName());

        // Update the product name
        int rowsUpdated = productTable.updateProductNameById("Updated Product Name", 1);

        // Verify update was successful
        assertEquals("Should update 1 row", 1, rowsUpdated);

        // Verify the product was updated
        productOpt = productTable.getProductById(1);
        assertTrue("Product with ID 1 should still exist", productOpt.isPresent());

        product = productOpt.get();
        assertEquals("Updated Product Name", product.getName());
    }

    @Test
    public void testUpdateActiveWhereId() {
        // Get a product to update
        Optional<Product> productOpt = productTable.getProductById(1);
        assertTrue("Product with ID 1 should exist", productOpt.isPresent());

        Product product = productOpt.get();
        assertTrue("Product should be active by default", product.isActive());

        // Update the product active status
        int rowsUpdated = productTable.updateProductActiveById(false, 1);

        // Verify update was successful
        assertEquals("Should update 1 row", 1, rowsUpdated);

        // Verify the product was updated
        productOpt = productTable.getProductById(1);
        assertTrue("Product with ID 1 should still exist", productOpt.isPresent());

        product = productOpt.get();
        assertFalse("Product should now be inactive", product.isActive());
    }

    @Test
    public void testUpdateNameActiveWhereId() {
        // Get a product to update
        Optional<Product> productOpt = productTable.getProductById(1);
        assertTrue("Product with ID 1 should exist", productOpt.isPresent());

        Product product = productOpt.get();
        assertEquals("Product 1", product.getName());
        assertTrue("Product should be active by default", product.isActive());

        // Update the product name and active status
        int rowsUpdated = productTable.updateProductNameAndActiveById("Updated Name and Status", false, 1);

        // Verify update was successful
        assertEquals("Should update 1 row", 1, rowsUpdated);

        // Verify the product was updated
        productOpt = productTable.getProductById(1);
        assertTrue("Product with ID 1 should still exist", productOpt.isPresent());

        product = productOpt.get();
        assertEquals("Updated Name and Status", product.getName());
        assertFalse("Product should now be inactive", product.isActive());
    }

    @Test
    public void testUpdateNameWhereLineId() {
        // First, ensure we have unique names for all products
        List<Product> allProducts = productTable.getAllProducts();
        for (int i = 0; i < allProducts.size(); i++) {
            Product p = allProducts.get(i);
            ContentValues values = new ContentValues();
            values.put("name", "Unique Product " + i);
            productTable.updateProductById(values, p.getId());
        }

        // Get products with line id 1
        List<Product> products = productTable.getAllProducts();
        List<Product> lineProducts = products.stream()
                .filter(p -> p.getLine() != null && p.getLine().getId() == 1)
                .collect(java.util.stream.Collectors.toList());

        assertFalse("Products with line ID 1 should exist", lineProducts.isEmpty());

        // Keep only one product with line id 1 to avoid unique constraint issues
        if (lineProducts.size() > 1) {
            // Update all but one product to have a different line id
            for (int i = 1; i < lineProducts.size(); i++) {
                Product p = lineProducts.get(i);
                ContentValues values = new ContentValues();
                values.put("line", 2); // Change to line id 2
                productTable.updateProductById(values, p.getId());
            }
        }

        // Get the updated list of products with line id 1
        products = productTable.getAllProducts();
        lineProducts = products.stream()
                .filter(p -> p.getLine() != null && p.getLine().getId() == 1)
                .collect(java.util.stream.Collectors.toList());

        assertEquals("Should be 1 product with line ID 1", 1, lineProducts.size());

        // Update the product name
        String newName = "Updated By Line ID";
        int rowsUpdated = productTable.updateProductNameByLineId(newName, 1);

        // Verify update was successful
        assertEquals("Should update 1 row", 1, rowsUpdated);

        // Verify the product was updated
        products = productTable.getAllProducts();
        lineProducts = products.stream()
                .filter(p -> p.getLine() != null && p.getLine().getId() == 1)
                .collect(java.util.stream.Collectors.toList());

        assertEquals("Should be 1 product with line ID 1", 1, lineProducts.size());
        assertEquals("Product name should be updated", newName, lineProducts.get(0).getName());
    }
}
