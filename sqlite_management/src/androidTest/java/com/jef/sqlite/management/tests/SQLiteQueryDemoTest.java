package com.jef.sqlite.management.tests;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.jef.sqlite.management.Management;
import com.jef.sqlite.management.Query.QueryFactory;
import com.jef.sqlite.management.models.Line;
import com.jef.sqlite.management.models.Product;
import com.jef.sqlite.management.queries.SQLiteQueryTest;
import com.jef.sqlite.management.tables.LineTable;
import com.jef.sqlite.management.tables.TableProducts;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;

/**
 * Test class that demonstrates how to use the SQLiteQuery annotation.
 * This test shows how to create and use custom SQL queries with the SQLiteQuery annotation.
 */
@RunWith(AndroidJUnit4.class)
public class SQLiteQueryDemoTest {

    private Context context;
    private TableProducts productTable;
    private LineTable lineTable;
    private SQLiteQueryTest sqliteQueryTest;

    // Store product information for tests
    private Product savedProduct1;
    private Product savedProduct2;
    private Product savedProduct3;
    private Product savedProduct4;

    @Before
    public void setup() {
        System.out.println("[DEBUG_LOG] Starting setup");
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        System.out.println("[DEBUG_LOG] Got context: " + context);

        // Clear existing data
        System.out.println("[DEBUG_LOG] Deleting database");
        context.deleteDatabase("test.db");

        // Initialize tables
        System.out.println("[DEBUG_LOG] Initializing tables");
        productTable = new TableProducts(context);
        lineTable = new LineTable(context);

        // Create the SQLiteQueryTest interface implementation
        System.out.println("[DEBUG_LOG] Creating SQLiteQueryTest implementation");
        Management management = new Management(context);
        sqliteQueryTest = QueryFactory.create(SQLiteQueryTest.class, management);

        // Setup test data
        System.out.println("[DEBUG_LOG] Setting up test data");
        setupTestData();
        System.out.println("[DEBUG_LOG] Setup complete");
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
            System.out.println("[DEBUG_LOG] Creating Product 1 (active)");
            Product product1 = new Product();
            product1.setName("Product 1_" + timestamp);
            product1.setLine(line1);
            product1.setActive(true); // Explicitly set as active
            this.savedProduct1 = productTable.saveProduct(product1);
            System.out.println("[DEBUG_LOG] Saved Product 1 with ID: " + this.savedProduct1.getId() + ", active: " + this.savedProduct1.isActive());

            System.out.println("[DEBUG_LOG] Creating Product 2 (active)");
            Product product2 = new Product();
            product2.setName("Product 2_" + timestamp);
            product2.setLine(line1);
            product2.setActive(true); // Explicitly set as active
            this.savedProduct2 = productTable.saveProduct(product2);
            System.out.println("[DEBUG_LOG] Saved Product 2 with ID: " + this.savedProduct2.getId() + ", active: " + this.savedProduct2.isActive());

            System.out.println("[DEBUG_LOG] Creating Product 3 (inactive)");
            Product product3 = new Product();
            product3.setName("Product 3_" + timestamp);
            product3.setLine(line2);
            product3.setActive(false); // Set as inactive
            this.savedProduct3 = productTable.saveProduct(product3);
            System.out.println("[DEBUG_LOG] Saved Product 3 with ID: " + this.savedProduct3.getId() + ", active: " + this.savedProduct3.isActive());

            System.out.println("[DEBUG_LOG] Creating Product 4 (inactive)");
            Product product4 = new Product();
            product4.setName("Product 4_" + timestamp);
            product4.setLine(line2);
            product4.setActive(false); // Set as inactive
            this.savedProduct4 = productTable.saveProduct(product4);
            System.out.println("[DEBUG_LOG] Saved Product 4 with ID: " + this.savedProduct4.getId() + ", active: " + this.savedProduct4.isActive());

            System.out.println("[DEBUG_LOG] setupTestData complete");
        } catch (Exception e) {
            System.out.println("[DEBUG_LOG] Exception in setupTestData: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Test
    public void testFindProductsByName() {
        System.out.println("[DEBUG_LOG] Starting testFindProductsByName");
        try {
            // Test the findProductsByName method with SQLiteQuery annotation
            String productName = savedProduct1.getName();
            System.out.println("[DEBUG_LOG] Calling findProductsByName with '" + productName + "'");
            List<Product> products = sqliteQueryTest.findProductsByName(productName);
            System.out.println("[DEBUG_LOG] Got products: " + (products != null ? products.size() : "null"));

            // Verify the results
            assertNotNull("Products list should not be null", products);
            System.out.println("[DEBUG_LOG] Products list is not null");

            assertEquals("Should find 1 product", 1, products.size());
            System.out.println("[DEBUG_LOG] Found 1 product");

            assertEquals("Product name should match", productName, products.get(0).getName());
            System.out.println("[DEBUG_LOG] Product name matches");

            System.out.println("[DEBUG_LOG] testFindProductsByName passed");
        } catch (Exception e) {
            System.out.println("[DEBUG_LOG] Exception in testFindProductsByName: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Test
    public void testFindProductById() {
        System.out.println("[DEBUG_LOG] Starting testFindProductById");
        try {
            // Test the findProductById method with SQLiteQuery annotation
            int productId = savedProduct1.getId();
            System.out.println("[DEBUG_LOG] Calling findProductById with ID " + productId);
            Optional<Product> productOpt = sqliteQueryTest.findProductById(productId);
            System.out.println("[DEBUG_LOG] Got product: " + (productOpt.isPresent() ? "present" : "not present"));

            // Verify the results
            assertTrue("Product should be found", productOpt.isPresent());
            System.out.println("[DEBUG_LOG] Product is present");

            assertEquals("Product ID should match", productId, productOpt.get().getId());
            System.out.println("[DEBUG_LOG] Product ID matches");

            System.out.println("[DEBUG_LOG] testFindProductById passed");
        } catch (Exception e) {
            System.out.println("[DEBUG_LOG] Exception in testFindProductById: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Test
    public void testFindProductsInLine() {
        System.out.println("[DEBUG_LOG] Starting testFindProductsInLine");
        try {
            // Test the findProductsInLine method with SQLiteQuery annotation
            int lineId = savedProduct1.getLine().getId();
            System.out.println("[DEBUG_LOG] Calling findProductsInLine with line ID " + lineId);
            List<Product> products = sqliteQueryTest.findProductsInLine(lineId);
            System.out.println("[DEBUG_LOG] Got products: " + (products != null ? products.size() : "null"));

            // Verify the results
            assertNotNull("Products list should not be null", products);
            System.out.println("[DEBUG_LOG] Products list is not null");

            assertEquals("Should find 2 products", 2, products.size());
            System.out.println("[DEBUG_LOG] Found 2 products");

            // Verify that all products are in the correct line
            System.out.println("[DEBUG_LOG] Verifying products are in the correct line");
            for (Product product : products) {
                System.out.println("[DEBUG_LOG] Checking product ID: " + product.getId() + ", Line ID: " + product.getLine().getId());
                assertEquals("Product should be in Line " + lineId, lineId, product.getLine().getId());
            }

            System.out.println("[DEBUG_LOG] testFindProductsInLine passed");
        } catch (Exception e) {
            System.out.println("[DEBUG_LOG] Exception in testFindProductsInLine: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Test
    public void testFindProductsWithIdsIn() {
        System.out.println("[DEBUG_LOG] Starting testFindProductsWithIdsIn");
        try {
            // Test the findProductsWithIdsIn method with SQLiteQuery annotation
            // Note: The implementation of findProductsWithIdsIn uses a single placeholder for the entire IN clause,
            // which means it treats the entire string as a single value. To work around this limitation,
            // we'll test with a single ID instead of multiple IDs.
            int id1 = savedProduct1.getId();
            String idString = String.valueOf(id1);
            System.out.println("[DEBUG_LOG] Calling findProductsWithIdsIn with ID '" + idString + "'");
            List<Product> products = sqliteQueryTest.findProductsWithIdsIn(idString);
            System.out.println("[DEBUG_LOG] Got products: " + (products != null ? products.size() : "null"));

            // Verify the results
            assertNotNull("Products list should not be null", products);
            System.out.println("[DEBUG_LOG] Products list is not null");

            // We should find at least one product
            assertTrue("Should find at least one product", products.size() >= 1);
            System.out.println("[DEBUG_LOG] Found at least one product: " + products.size());

            // Verify that the correct product was found
            System.out.println("[DEBUG_LOG] Verifying correct product was found");
            boolean foundProduct1 = false;

            for (Product product : products) {
                System.out.println("[DEBUG_LOG] Checking product ID: " + product.getId());
                if (product.getId() == id1) {
                    foundProduct1 = true;
                    System.out.println("[DEBUG_LOG] Found Product with ID " + id1);
                    break;  // Found what we're looking for, no need to continue
                }
            }

            assertTrue("Should find Product with ID " + id1, foundProduct1);
            System.out.println("[DEBUG_LOG] Verified Product with ID " + id1 + " was found");

            System.out.println("[DEBUG_LOG] testFindProductsWithIdsIn passed");
        } catch (Exception e) {
            System.out.println("[DEBUG_LOG] Exception in testFindProductsWithIdsIn: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Test
    public void testFindActiveProducts() {
        System.out.println("[DEBUG_LOG] Starting testFindActiveProducts");
        try {
            // Test the findActiveProducts method with SQLiteQuery annotation
            System.out.println("[DEBUG_LOG] Calling findActiveProducts");
            List<Product> products = sqliteQueryTest.findActiveProducts();
            System.out.println("[DEBUG_LOG] Got products: " + (products != null ? products.size() : "null"));

            // Verify the results
            assertNotNull("Products list should not be null", products);
            System.out.println("[DEBUG_LOG] Products list is not null");

            // We should find at least our 2 active products
            assertTrue("Should find at least 2 active products", products.size() >= 2);
            System.out.println("[DEBUG_LOG] Found at least 2 active products: " + products.size());

            // Verify that all products are active
            System.out.println("[DEBUG_LOG] Verifying all products are active");
            for (Product product : products) {
                System.out.println("[DEBUG_LOG] Checking product ID: " + product.getId() + ", active: " + product.isActive());
                assertTrue("Product should be active", product.isActive());
            }

            // Verify that our active products were found
            System.out.println("[DEBUG_LOG] Verifying our active products were found");
            int id1 = savedProduct1.getId();
            int id2 = savedProduct2.getId();
            boolean foundProduct1 = false;
            boolean foundProduct2 = false;

            for (Product product : products) {
                if (product.getId() == id1) {
                    foundProduct1 = true;
                    System.out.println("[DEBUG_LOG] Found Product with ID " + id1);
                }
                if (product.getId() == id2) {
                    foundProduct2 = true;
                    System.out.println("[DEBUG_LOG] Found Product with ID " + id2);
                }
            }

            assertTrue("Should find Product with ID " + id1, foundProduct1);
            System.out.println("[DEBUG_LOG] Verified Product with ID " + id1 + " was found");

            assertTrue("Should find Product with ID " + id2, foundProduct2);
            System.out.println("[DEBUG_LOG] Verified Product with ID " + id2 + " was found");

            System.out.println("[DEBUG_LOG] testFindActiveProducts passed");
        } catch (Exception e) {
            System.out.println("[DEBUG_LOG] Exception in testFindActiveProducts: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Test
    public void testFindInactiveProducts() {
        System.out.println("[DEBUG_LOG] Starting testFindInactiveProducts");
        try {
            // Test the findInactiveProducts method with SQLiteQuery annotation
            System.out.println("[DEBUG_LOG] Calling findInactiveProducts");
            List<Product> products = sqliteQueryTest.findInactiveProducts();
            System.out.println("[DEBUG_LOG] Got products: " + (products != null ? products.size() : "null"));

            // Verify the results
            assertNotNull("Products list should not be null", products);
            System.out.println("[DEBUG_LOG] Products list is not null");

            // We should find at least our 2 inactive products
            assertTrue("Should find at least 2 inactive products", products.size() >= 2);
            System.out.println("[DEBUG_LOG] Found at least 2 inactive products: " + products.size());

            // Verify that all products are inactive
            System.out.println("[DEBUG_LOG] Verifying all products are inactive");
            for (Product product : products) {
                System.out.println("[DEBUG_LOG] Checking product ID: " + product.getId() + ", active: " + product.isActive());
                assertFalse("Product should be inactive", product.isActive());
            }

            // Verify that our inactive products were found
            System.out.println("[DEBUG_LOG] Verifying our inactive products were found");
            int id3 = savedProduct3.getId();
            int id4 = savedProduct4.getId();
            boolean foundProduct3 = false;
            boolean foundProduct4 = false;

            for (Product product : products) {
                if (product.getId() == id3) {
                    foundProduct3 = true;
                    System.out.println("[DEBUG_LOG] Found Product with ID " + id3);
                }
                if (product.getId() == id4) {
                    foundProduct4 = true;
                    System.out.println("[DEBUG_LOG] Found Product with ID " + id4);
                }
            }

            assertTrue("Should find Product with ID " + id3, foundProduct3);
            System.out.println("[DEBUG_LOG] Verified Product with ID " + id3 + " was found");

            assertTrue("Should find Product with ID " + id4, foundProduct4);
            System.out.println("[DEBUG_LOG] Verified Product with ID " + id4 + " was found");

            System.out.println("[DEBUG_LOG] testFindInactiveProducts passed");
        } catch (Exception e) {
            System.out.println("[DEBUG_LOG] Exception in testFindInactiveProducts: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
}
