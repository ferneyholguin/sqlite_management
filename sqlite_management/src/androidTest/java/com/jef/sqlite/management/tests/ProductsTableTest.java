package com.jef.sqlite.management.tests;

import android.content.ContentValues;
import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.jef.sqlite.management.models.Line;
import com.jef.sqlite.management.models.Product;
import com.jef.sqlite.management.tables.LineTable;
import com.jef.sqlite.management.tables.ProductsTable;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;

/**
 * Instrumented test for ProductsTable class.
 */
@RunWith(AndroidJUnit4.class)
public class ProductsTableTest {

    private ProductsTable productsTable;
    private LineTable lineTable;
    private Context context;
    private Line testLine;

    @Before
    public void setUp() {
        // Get the context for the test
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        // Initialize the tables
        productsTable = new ProductsTable(context);
        lineTable = new LineTable(context);

        // Create a test line to use with products
        Line line = new Line();
        line.setName("Test Line for Products");
        testLine = lineTable.saveLine(line);

        // Clean up any existing test data
        cleanUpTestData();
    }

    @After
    public void tearDown() {
        // Clean up test data
        cleanUpTestData();
    }

    private void cleanUpTestData() {
        // Delete test products
        List<Product> products = productsTable.getAllProducts();
        for (Product product : products) {
            if (product.getName() != null && product.getName().startsWith("Test Product")) {
                productsTable.deleteById(product.getId());
            }
        }
    }

    @Test
    public void testGetProductsByName() {
        // Create and save a test product
        Product product = new Product();
        product.setName("Test Product GetByName");
        product.setLine(testLine);
        Product savedProduct = productsTable.saveProduct(product);

        // Get products by name
        List<Product> products = productsTable.getProductsByName("Test Product GetByName");

        // Verify the product was found
        assertFalse(products.isEmpty());
        assertEquals(1, products.size());
        assertEquals(savedProduct.getId(), products.get(0).getId());
        assertEquals("Test Product GetByName", products.get(0).getName());
    }

    @Test
    public void testGetProductById() {
        // Create and save a test product
        Product product = new Product();
        product.setName("Test Product GetById");
        product.setLine(testLine);
        Product savedProduct = productsTable.saveProduct(product);

        // Get product by ID
        Optional<Product> retrievedProduct = productsTable.getProductById(savedProduct.getId());

        // Verify the product was found
        assertTrue(retrievedProduct.isPresent());
        assertEquals(savedProduct.getId(), retrievedProduct.get().getId());
        assertEquals("Test Product GetById", retrievedProduct.get().getName());
        assertEquals(testLine.getId(), retrievedProduct.get().getLine().getId());
    }

    @Test
    public void testGetAllProducts() {
        // Create and save a test product
        Product product = new Product();
        product.setName("Test Product GetAll");
        product.setLine(testLine);
        Product savedProduct = productsTable.saveProduct(product);

        // Get all products
        List<Product> products = productsTable.getAllProducts();

        // Verify the product is in the list
        boolean found = false;
        for (Product p : products) {
            if (p.getId() == savedProduct.getId()) {
                found = true;
                assertEquals("Test Product GetAll", p.getName());
                assertEquals(testLine.getId(), p.getLine().getId());
                break;
            }
        }

        assertTrue("Test product should be found in the list", found);
    }

    @Test
    public void testGetAllProductsOrderedByName() {
        // Create and save multiple test products with different names
        Product product1 = new Product();
        product1.setName("Test Product A");
        product1.setLine(testLine);
        productsTable.saveProduct(product1);

        Product product2 = new Product();
        product2.setName("Test Product B");
        product2.setLine(testLine);
        productsTable.saveProduct(product2);

        Product product3 = new Product();
        product3.setName("Test Product C");
        product3.setLine(testLine);
        productsTable.saveProduct(product3);

        // Get products ordered by name ascending
        List<Product> productsAsc = productsTable.getAllProductsOrderedByName(true);

        // Verify the order is correct (A, B, C)
        int indexA = -1, indexB = -1, indexC = -1;
        for (int i = 0; i < productsAsc.size(); i++) {
            Product p = productsAsc.get(i);
            if ("Test Product A".equals(p.getName())) indexA = i;
            if ("Test Product B".equals(p.getName())) indexB = i;
            if ("Test Product C".equals(p.getName())) indexC = i;
        }

        assertTrue("All test products should be found", indexA >= 0 && indexB >= 0 && indexC >= 0);
        assertTrue("Products should be in ascending order", indexA < indexB && indexB < indexC);

        // Get products ordered by name descending
        List<Product> productsDesc = productsTable.getAllProductsOrderedByName(false);

        // Reset indices
        indexA = -1; indexB = -1; indexC = -1;
        for (int i = 0; i < productsDesc.size(); i++) {
            Product p = productsDesc.get(i);
            if ("Test Product A".equals(p.getName())) indexA = i;
            if ("Test Product B".equals(p.getName())) indexB = i;
            if ("Test Product C".equals(p.getName())) indexC = i;
        }

        assertTrue("All test products should be found", indexA >= 0 && indexB >= 0 && indexC >= 0);
        assertTrue("Products should be in descending order", indexA > indexB && indexB > indexC);
    }

    @Test
    public void testSaveProduct() {
        // Create a new product
        Product product = new Product();
        product.setName("Test Product Save");
        product.setActive(true);
        product.setLine(testLine);

        // Save the product
        Product savedProduct = productsTable.saveProduct(product);

        // Verify the product was saved with an ID
        assertNotEquals(0, savedProduct.getId());
        assertEquals("Test Product Save", savedProduct.getName());
        assertTrue(savedProduct.isActive());
        assertEquals(testLine.getId(), savedProduct.getLine().getId());

        // Verify we can retrieve the saved product
        Optional<Product> retrievedProduct = productsTable.getProductById(savedProduct.getId());
        assertTrue(retrievedProduct.isPresent());
        assertEquals(savedProduct.getId(), retrievedProduct.get().getId());
        assertEquals(savedProduct.getName(), retrievedProduct.get().getName());
        assertEquals(savedProduct.isActive(), retrievedProduct.get().isActive());
        assertEquals(savedProduct.getLine().getId(), retrievedProduct.get().getLine().getId());
    }

    @Test
    public void testUpdateProductById() {
        // Create and save a test product
        Product product = new Product();
        product.setName("Test Product UpdateById");
        product.setLine(testLine);
        Product savedProduct = productsTable.saveProduct(product);

        // Create content values for update
        ContentValues values = new ContentValues();
        values.put("name", "Updated Product Name");
        values.put("active", 0); // false

        // Update the product
        int rowsUpdated = productsTable.updateProductById(values, savedProduct.getId());

        // Verify the update was successful
        assertEquals(1, rowsUpdated);

        // Verify the product was updated
        Optional<Product> updatedProduct = productsTable.getProductById(savedProduct.getId());
        assertTrue(updatedProduct.isPresent());
        assertEquals("Updated Product Name", updatedProduct.get().getName());
        assertFalse(updatedProduct.get().isActive());
    }

    @Test
    public void testUpdateProductNameById() {
        // Create and save a test product
        Product product = new Product();
        product.setName("Test Product UpdateNameById");
        product.setLine(testLine);
        Product savedProduct = productsTable.saveProduct(product);

        // Update the product's name
        int rowsUpdated = productsTable.updateProductNameById("Updated Name", savedProduct.getId());

        // Verify the update was successful
        assertEquals(1, rowsUpdated);

        // Verify the product's name was updated
        Optional<Product> updatedProduct = productsTable.getProductById(savedProduct.getId());
        assertTrue(updatedProduct.isPresent());
        assertEquals("Updated Name", updatedProduct.get().getName());
    }

    @Test
    public void testUpdateProductActiveById() {
        // Create and save a test product
        Product product = new Product();
        product.setName("Test Product UpdateActiveById");
        product.setActive(true);
        product.setLine(testLine);
        Product savedProduct = productsTable.saveProduct(product);

        // Update the product's active status
        int rowsUpdated = productsTable.updateProductActiveById(false, savedProduct.getId());

        // Verify the update was successful
        assertEquals(1, rowsUpdated);

        // Verify the product's active status was updated
        Optional<Product> updatedProduct = productsTable.getProductById(savedProduct.getId());
        assertTrue(updatedProduct.isPresent());
        assertFalse(updatedProduct.get().isActive());
    }

    @Test
    public void testValidateProduct() {
        // Create a valid product
        Product validProduct = new Product();
        validProduct.setName("Test Product Validate");
        validProduct.setLine(testLine);

        // Validate the product
        boolean isValid = productsTable.validateProduct(validProduct);

        // Verify the product is valid
        assertTrue(isValid);

        // Create an invalid product (no name)
        Product invalidProduct = new Product();
        invalidProduct.setLine(testLine);

        // Validate the product
        boolean isInvalid = productsTable.validateProduct(invalidProduct);

        // Verify the product is invalid
        assertFalse(isInvalid);
    }

    @Test
    public void testGetProductsByNameAndActive() {
        // Create and save test products with different active statuses but different names
        // to avoid unique constraint violation
        String activeName = "Test Product NameAndActive Active";
        String inactiveName = "Test Product NameAndActive Inactive";

        Product activeProduct = new Product();
        activeProduct.setName(activeName);
        activeProduct.setActive(true);
        activeProduct.setLine(testLine);
        productsTable.saveProduct(activeProduct);

        Product inactiveProduct = new Product();
        inactiveProduct.setName(inactiveName);
        inactiveProduct.setActive(false);
        inactiveProduct.setLine(testLine);
        productsTable.saveProduct(inactiveProduct);

        // Get active products by exact name match
        List<Product> activeProducts = productsTable.getProductsByNameAndActive(activeName, true);

        // Verify only active products are returned
        assertFalse("Active products list should not be empty", activeProducts.isEmpty());
        for (Product p : activeProducts) {
            assertEquals(activeName, p.getName());
            assertTrue(p.isActive());
        }

        // Get inactive products by exact name match
        List<Product> inactiveProducts = productsTable.getProductsByNameAndActive(inactiveName, false);

        // Verify only inactive products are returned
        assertFalse("Inactive products list should not be empty", inactiveProducts.isEmpty());
        for (Product p : inactiveProducts) {
            assertEquals(inactiveName, p.getName());
            assertFalse(p.isActive());
        }
    }

    @Test
    public void testProductExistsById() {
        // Create and save a test product
        Product product = new Product();
        product.setName("Test Product ExistsById");
        product.setLine(testLine);
        Product savedProduct = productsTable.saveProduct(product);

        // Check if the product exists by ID
        boolean exists = productsTable.productExistsById(savedProduct.getId());

        // Verify the product exists
        assertTrue(exists);

        // Check if a non-existent product exists
        boolean notExists = productsTable.productExistsById(-1);

        // Verify the product does not exist
        assertFalse(notExists);
    }

    @Test
    public void testDeleteById() {
        // Create and save a test product
        Product product = new Product();
        product.setName("Test Product DeleteById");
        product.setLine(testLine);
        Product savedProduct = productsTable.saveProduct(product);

        // Verify the product exists
        assertTrue(productsTable.productExistsById(savedProduct.getId()));

        // Delete the product
        int rowsDeleted = productsTable.deleteById(savedProduct.getId());

        // Verify the delete was successful
        assertEquals(1, rowsDeleted);

        // Verify the product no longer exists
        assertFalse(productsTable.productExistsById(savedProduct.getId()));
    }
}
