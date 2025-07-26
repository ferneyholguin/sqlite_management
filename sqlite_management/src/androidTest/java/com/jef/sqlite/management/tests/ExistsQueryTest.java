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

import static org.junit.Assert.*;

/**
 * Test class that demonstrates how to use the existsBy* methods.
 * This test shows how to use dynamic query methods to check if records exist in the database.
 */
@RunWith(AndroidJUnit4.class)
public class ExistsQueryTest {

    private Context context;
    private TableProducts productTable;
    private LineTable lineTable;
    private SQLiteQueryTest sqliteQueryTest;

    // Store product names with unique suffixes
    private String product1Name;
    private String product2Name;
    private String product3Name;
    private String product4Name;
    private int product1Id;

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

            // Create a unique suffix for product names to avoid UNIQUE constraint violations
            String uniqueSuffix = "_" + System.currentTimeMillis();
            System.out.println("[DEBUG_LOG] Using unique suffix for product names: " + uniqueSuffix);

            // Create lines
            System.out.println("[DEBUG_LOG] Creating Line 1");
            Line line1 = new Line();
            line1.setName("Line 1" + uniqueSuffix);
            line1 = lineTable.saveLine(line1);
            System.out.println("[DEBUG_LOG] Saved Line 1 with ID: " + line1.getId());

            System.out.println("[DEBUG_LOG] Creating Line 2");
            Line line2 = new Line();
            line2.setName("Line 2" + uniqueSuffix);
            line2 = lineTable.saveLine(line2);
            System.out.println("[DEBUG_LOG] Saved Line 2 with ID: " + line2.getId());

            // Create products
            System.out.println("[DEBUG_LOG] Creating Product 1 (active)");
            Product product1 = new Product();
            product1Name = "Product 1" + uniqueSuffix;
            product1.setName(product1Name);
            product1.setLine(line1);
            product1.setActive(true);
            Product savedProduct1 = productTable.saveProduct(product1);
            product1Id = savedProduct1.getId();
            System.out.println("[DEBUG_LOG] Saved Product 1 with ID: " + product1Id + ", active: " + savedProduct1.isActive());

            System.out.println("[DEBUG_LOG] Creating Product 2 (active)");
            Product product2 = new Product();
            product2Name = "Product 2" + uniqueSuffix;
            product2.setName(product2Name);
            product2.setLine(line1);
            product2.setActive(true);
            Product savedProduct2 = productTable.saveProduct(product2);
            System.out.println("[DEBUG_LOG] Saved Product 2 with ID: " + savedProduct2.getId() + ", active: " + savedProduct2.isActive());

            System.out.println("[DEBUG_LOG] Creating Product 3 (inactive)");
            Product product3 = new Product();
            product3Name = "Product 3" + uniqueSuffix;
            product3.setName(product3Name);
            product3.setLine(line2);
            product3.setActive(false);
            Product savedProduct3 = productTable.saveProduct(product3);
            System.out.println("[DEBUG_LOG] Saved Product 3 with ID: " + savedProduct3.getId() + ", active: " + savedProduct3.isActive());

            System.out.println("[DEBUG_LOG] Creating Product 4 (inactive)");
            Product product4 = new Product();
            product4Name = "Product 4" + uniqueSuffix;
            product4.setName(product4Name);
            product4.setLine(line2);
            product4.setActive(false);
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
    public void testExistsById() {
        System.out.println("[DEBUG_LOG] Starting testExistsById");
        try {
            // Test existsById with an existing ID
            System.out.println("[DEBUG_LOG] Checking if product with ID " + product1Id + " exists");
            boolean exists = sqliteQueryTest.existsById(product1Id);
            System.out.println("[DEBUG_LOG] Product with ID " + product1Id + " exists: " + exists);

            // Verify the result
            assertTrue("Product with ID " + product1Id + " should exist", exists);

            // Test existsById with a non-existing ID
            int nonExistingId = 999999;
            System.out.println("[DEBUG_LOG] Checking if product with ID " + nonExistingId + " exists");
            exists = sqliteQueryTest.existsById(nonExistingId);
            System.out.println("[DEBUG_LOG] Product with ID " + nonExistingId + " exists: " + exists);

            // Verify the result
            assertFalse("Product with ID " + nonExistingId + " should not exist", exists);

            System.out.println("[DEBUG_LOG] testExistsById passed");
        } catch (Exception e) {
            System.out.println("[DEBUG_LOG] Exception in testExistsById: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Test
    public void testExistsByName() {
        System.out.println("[DEBUG_LOG] Starting testExistsByName");
        try {
            // Test existsByName with an existing name
            System.out.println("[DEBUG_LOG] Checking if product with name '" + product1Name + "' exists");
            boolean exists = sqliteQueryTest.existsByName(product1Name);
            System.out.println("[DEBUG_LOG] Product with name '" + product1Name + "' exists: " + exists);

            // Verify the result
            assertTrue("Product with name '" + product1Name + "' should exist", exists);

            // Test existsByName with a non-existing name
            String nonExistingName = "Nonexistent Product " + System.currentTimeMillis();
            System.out.println("[DEBUG_LOG] Checking if product with name '" + nonExistingName + "' exists");
            exists = sqliteQueryTest.existsByName(nonExistingName);
            System.out.println("[DEBUG_LOG] Product with name '" + nonExistingName + "' exists: " + exists);

            // Verify the result
            assertFalse("Product with name '" + nonExistingName + "' should not exist", exists);

            System.out.println("[DEBUG_LOG] testExistsByName passed");
        } catch (Exception e) {
            System.out.println("[DEBUG_LOG] Exception in testExistsByName: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Test
    public void testExistsByActive() {
        System.out.println("[DEBUG_LOG] Starting testExistsByActive");
        try {
            // Test existsByActive with active=true
            System.out.println("[DEBUG_LOG] Checking if active products exist");
            boolean exists = sqliteQueryTest.existsByActive(true);
            System.out.println("[DEBUG_LOG] Active products exist: " + exists);

            // Verify the result
            assertTrue("Active products should exist", exists);

            // Test existsByActive with active=false
            System.out.println("[DEBUG_LOG] Checking if inactive products exist");
            exists = sqliteQueryTest.existsByActive(false);
            System.out.println("[DEBUG_LOG] Inactive products exist: " + exists);

            // Verify the result
            assertTrue("Inactive products should exist", exists);

            System.out.println("[DEBUG_LOG] testExistsByActive passed");
        } catch (Exception e) {
            System.out.println("[DEBUG_LOG] Exception in testExistsByActive: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Test
    public void testExistsByNameAndActive() {
        System.out.println("[DEBUG_LOG] Starting testExistsByNameAndActive");
        try {
            // Test existsByNameAndActive with an existing combination
            System.out.println("[DEBUG_LOG] Checking if product with name 'Product 1' and active=true exists");
            boolean exists = sqliteQueryTest.existsByNameAndActive("Product 1", true);
            System.out.println("[DEBUG_LOG] Product with name 'Product 1' and active=true exists: " + exists);

            // Verify the result
            assertTrue("Product with name 'Product 1' and active=true should exist", exists);

            // Test existsByNameAndActive with a non-existing combination
            System.out.println("[DEBUG_LOG] Checking if product with name 'Product 1' and active=false exists");
            exists = sqliteQueryTest.existsByNameAndActive("Product 1", false);
            System.out.println("[DEBUG_LOG] Product with name 'Product 1' and active=false exists: " + exists);

            // Verify the result
            assertFalse("Product with name 'Product 1' and active=false should not exist", exists);

            // Test another existing combination
            System.out.println("[DEBUG_LOG] Checking if product with name 'Product 3' and active=false exists");
            exists = sqliteQueryTest.existsByNameAndActive("Product 3", false);
            System.out.println("[DEBUG_LOG] Product with name 'Product 3' and active=false exists: " + exists);

            // Verify the result
            assertTrue("Product with name 'Product 3' and active=false should exist", exists);

            System.out.println("[DEBUG_LOG] testExistsByNameAndActive passed");
        } catch (Exception e) {
            System.out.println("[DEBUG_LOG] Exception in testExistsByNameAndActive: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
}
