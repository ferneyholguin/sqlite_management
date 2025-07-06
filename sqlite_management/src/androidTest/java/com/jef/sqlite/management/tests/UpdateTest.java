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

        // Clear existing data
        context.deleteDatabase("test.db");

        // Initialize tables
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
        // Get products with line id 1
        List<Product> products = productTable.getAllProducts();
        List<Product> lineProducts = products.stream()
                .filter(p -> p.getLine() != null && p.getLine().getId() == 1)
                .collect(java.util.stream.Collectors.toList());

        assertFalse("Products with line ID 1 should exist", lineProducts.isEmpty());
        assertEquals("Should be 2 products with line ID 1", 2, lineProducts.size());

        // Update the products
        int rowsUpdated = productTable.updateProductNameByLineId("Updated By Line ID", 1);

        // Verify update was successful
        assertEquals("Should update 2 rows", 2, rowsUpdated);

        // Verify the products were updated
        products = productTable.getAllProducts();
        lineProducts = products.stream()
                .filter(p -> p.getLine() != null && p.getLine().getId() == 1)
                .collect(java.util.stream.Collectors.toList());

        for (Product p : lineProducts) {
            assertEquals("Product name should be updated", "Updated By Line ID", p.getName());
        }
    }
}
