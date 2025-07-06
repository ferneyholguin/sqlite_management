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

            // Create lines
            System.out.println("[DEBUG_LOG] Creating Line 1");
            Line line1 = new Line();
            line1.setName("Line 1");
            line1 = lineTable.saveLine(line1);
            System.out.println("[DEBUG_LOG] Saved Line 1 with ID: " + line1.getId());

            System.out.println("[DEBUG_LOG] Creating Line 2");
            Line line2 = new Line();
            line2.setName("Line 2");
            line2 = lineTable.saveLine(line2);
            System.out.println("[DEBUG_LOG] Saved Line 2 with ID: " + line2.getId());

            // Create products
            System.out.println("[DEBUG_LOG] Creating Product 1 (active)");
            Product product1 = new Product();
            product1.setName("Product 1");
            product1.setLine(line1);
            product1.setActive(true); // Explicitly set as active
            Product savedProduct1 = productTable.saveProduct(product1);
            System.out.println("[DEBUG_LOG] Saved Product 1 with ID: " + savedProduct1.getId() + ", active: " + savedProduct1.isActive());

            System.out.println("[DEBUG_LOG] Creating Product 2 (active)");
            Product product2 = new Product();
            product2.setName("Product 2");
            product2.setLine(line1);
            product2.setActive(true); // Explicitly set as active
            Product savedProduct2 = productTable.saveProduct(product2);
            System.out.println("[DEBUG_LOG] Saved Product 2 with ID: " + savedProduct2.getId() + ", active: " + savedProduct2.isActive());

            System.out.println("[DEBUG_LOG] Creating Product 3 (inactive)");
            Product product3 = new Product();
            product3.setName("Product 3");
            product3.setLine(line2);
            product3.setActive(false); // Set as inactive
            Product savedProduct3 = productTable.saveProduct(product3);
            System.out.println("[DEBUG_LOG] Saved Product 3 with ID: " + savedProduct3.getId() + ", active: " + savedProduct3.isActive());

            System.out.println("[DEBUG_LOG] Creating Product 4 (inactive)");
            Product product4 = new Product();
            product4.setName("Product 4");
            product4.setLine(line2);
            product4.setActive(false); // Set as inactive
            Product savedProduct4 = productTable.saveProduct(product4);
            System.out.println("[DEBUG_LOG] Saved Product 4 with ID: " + savedProduct4.getId() + ", active: " + savedProduct4.isActive());

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
            System.out.println("[DEBUG_LOG] Calling findProductsByName with 'Product 1'");
            List<Product> products = sqliteQueryTest.findProductsByName("Product 1");
            System.out.println("[DEBUG_LOG] Got products: " + (products != null ? products.size() : "null"));

            // Verify the results
            assertNotNull("Products list should not be null", products);
            System.out.println("[DEBUG_LOG] Products list is not null");

            assertEquals("Should find 1 product", 1, products.size());
            System.out.println("[DEBUG_LOG] Found 1 product");

            assertEquals("Product name should match", "Product 1", products.get(0).getName());
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
            System.out.println("[DEBUG_LOG] Calling findProductById with ID 1");
            Optional<Product> productOpt = sqliteQueryTest.findProductById(1);
            System.out.println("[DEBUG_LOG] Got product: " + (productOpt.isPresent() ? "present" : "not present"));

            // Verify the results
            assertTrue("Product should be found", productOpt.isPresent());
            System.out.println("[DEBUG_LOG] Product is present");

            assertEquals("Product ID should match", 1, productOpt.get().getId());
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
            System.out.println("[DEBUG_LOG] Calling findProductsInLine with line ID 1");
            List<Product> products = sqliteQueryTest.findProductsInLine(1);
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
                assertEquals("Product should be in Line 1", 1, product.getLine().getId());
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
            System.out.println("[DEBUG_LOG] Calling findProductsWithIdsIn with IDs '1,2'");
            List<Product> products = sqliteQueryTest.findProductsWithIdsIn("1,2");
            System.out.println("[DEBUG_LOG] Got products: " + (products != null ? products.size() : "null"));

            // Verify the results
            assertNotNull("Products list should not be null", products);
            System.out.println("[DEBUG_LOG] Products list is not null");

            assertEquals("Should find 2 products", 2, products.size());
            System.out.println("[DEBUG_LOG] Found 2 products");

            // Verify that the correct products were found
            System.out.println("[DEBUG_LOG] Verifying correct products were found");
            boolean foundProduct1 = false;
            boolean foundProduct2 = false;

            for (Product product : products) {
                System.out.println("[DEBUG_LOG] Checking product ID: " + product.getId());
                if (product.getId() == 1) {
                    foundProduct1 = true;
                    System.out.println("[DEBUG_LOG] Found Product 1");
                }
                if (product.getId() == 2) {
                    foundProduct2 = true;
                    System.out.println("[DEBUG_LOG] Found Product 2");
                }
            }

            assertTrue("Should find Product 1", foundProduct1);
            System.out.println("[DEBUG_LOG] Verified Product 1 was found");

            assertTrue("Should find Product 2", foundProduct2);
            System.out.println("[DEBUG_LOG] Verified Product 2 was found");

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

            assertEquals("Should find 2 active products", 2, products.size());
            System.out.println("[DEBUG_LOG] Found 2 active products");

            // Verify that all products are active
            System.out.println("[DEBUG_LOG] Verifying all products are active");
            for (Product product : products) {
                System.out.println("[DEBUG_LOG] Checking product ID: " + product.getId() + ", active: " + product.isActive());
                assertTrue("Product should be active", product.isActive());
            }

            // Verify that the correct products were found
            System.out.println("[DEBUG_LOG] Verifying correct products were found");
            boolean foundProduct1 = false;
            boolean foundProduct2 = false;

            for (Product product : products) {
                if (product.getId() == 1) {
                    foundProduct1 = true;
                    System.out.println("[DEBUG_LOG] Found Product 1");
                }
                if (product.getId() == 2) {
                    foundProduct2 = true;
                    System.out.println("[DEBUG_LOG] Found Product 2");
                }
            }

            assertTrue("Should find Product 1", foundProduct1);
            System.out.println("[DEBUG_LOG] Verified Product 1 was found");

            assertTrue("Should find Product 2", foundProduct2);
            System.out.println("[DEBUG_LOG] Verified Product 2 was found");

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

            assertEquals("Should find 2 inactive products", 2, products.size());
            System.out.println("[DEBUG_LOG] Found 2 inactive products");

            // Verify that all products are inactive
            System.out.println("[DEBUG_LOG] Verifying all products are inactive");
            for (Product product : products) {
                System.out.println("[DEBUG_LOG] Checking product ID: " + product.getId() + ", active: " + product.isActive());
                assertFalse("Product should be inactive", product.isActive());
            }

            // Verify that the correct products were found
            System.out.println("[DEBUG_LOG] Verifying correct products were found");
            boolean foundProduct3 = false;
            boolean foundProduct4 = false;

            for (Product product : products) {
                if (product.getId() == 3) {
                    foundProduct3 = true;
                    System.out.println("[DEBUG_LOG] Found Product 3");
                }
                if (product.getId() == 4) {
                    foundProduct4 = true;
                    System.out.println("[DEBUG_LOG] Found Product 4");
                }
            }

            assertTrue("Should find Product 3", foundProduct3);
            System.out.println("[DEBUG_LOG] Verified Product 3 was found");

            assertTrue("Should find Product 4", foundProduct4);
            System.out.println("[DEBUG_LOG] Verified Product 4 was found");

            System.out.println("[DEBUG_LOG] testFindInactiveProducts passed");
        } catch (Exception e) {
            System.out.println("[DEBUG_LOG] Exception in testFindInactiveProducts: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
}
