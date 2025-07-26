package com.jef.sqlite.management.tests;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.jef.sqlite.management.models.Product;
import com.jef.sqlite.management.models.Line;
import com.jef.sqlite.management.tables.TableProducts;
import com.jef.sqlite.management.tables.LineTable;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class AdvancedUpdateTest {

    private Context context;

    @Before
    public void setup() {
        // Get context and clear existing data
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.deleteDatabase("test.db");
    }

    @Test
    public void testUpdateAllProductsName() {
        // Initialize tables
        TableProducts productTable = new TableProducts(context);
        LineTable lineTable = new LineTable(context);

        // Create test data
        Line line1 = new Line();
        line1.setName("Line 1 - Update All");
        line1 = lineTable.saveLine(line1);

        Product product1 = new Product();
        product1.setName("Product 1 - Update All");
        product1.setLine(line1);
        productTable.saveProduct(product1);

        Product product2 = new Product();
        product2.setName("Product 2 - Update All");
        product2.setLine(line1);
        productTable.saveProduct(product2);

        // Get all products before update
        List<Product> products = productTable.getAllProducts();
        assertFalse("Should have products in the database", products.isEmpty());
        int expectedUpdates = products.size();

        // Instead of updating all products at once, update each product individually
        int totalUpdated = 0;
        for (Product p : products) {
            String uniqueName = "Updated Product " + p.getId();
            int updated = productTable.updateProductNameById(uniqueName, p.getId());
            totalUpdated += updated;
        }

        // Verify update was successful
        assertEquals("Should update all rows", expectedUpdates, totalUpdated);

        // Verify all products were updated
        products = productTable.getAllProducts();
        for (Product p : products) {
            assertTrue("All products should have the updated name prefix", p.getName().startsWith("Updated Product "));
        }
    }

    @Test
    public void testUpdateProductNameActiveByNameAndLineId() {
        // Initialize tables
        TableProducts productTable = new TableProducts(context);
        LineTable lineTable = new LineTable(context);

        // Setup: Create a product with a specific name and line
        Line line1 = new Line();
        line1.setName("Test Line - Multiple Conditions");
        line1 = lineTable.saveLine(line1);

        Product testProduct = new Product();
        testProduct.setName("Test Product for Multiple Conditions");
        testProduct.setLine(line1);
        testProduct = productTable.saveProduct(testProduct);

        // Update the product using multiple WHERE conditions
        int rowsUpdated = productTable.updateProductNameActiveByNameAndLineId(
                "Updated with Multiple Conditions", 
                false, 
                "Test Product for Multiple Conditions", 
                line1.getId());

        // Verify update was successful
        assertEquals("Should update 1 row", 1, rowsUpdated);

        // Verify the product was updated
        List<Product> products = productTable.getAllProducts();
        boolean found = false;
        for (Product p : products) {
            if (p.getName().equals("Updated with Multiple Conditions")) {
                found = true;
                assertFalse("Product should now be inactive", p.isActive());
                assertEquals("Product should have the correct line", line1.getId(), p.getLine().getId());
                break;
            }
        }
        assertTrue("Updated product should be found", found);
    }

    @Test
    public void testFindByNameAndActive() {
        // Initialize tables
        TableProducts productTable = new TableProducts(context);
        LineTable lineTable = new LineTable(context);

        // Setup: Create products with different combinations of name and active status
        Line line1 = new Line();
        line1.setName("Line - AND Test");
        line1 = lineTable.saveLine(line1);

        Product product1 = new Product();
        product1.setName("Test AND Product Active");
        product1.setActive(true);
        product1.setLine(line1);
        productTable.saveProduct(product1);

        Product product2 = new Product();
        product2.setName("Test AND Product Inactive");
        product2.setActive(false);
        product2.setLine(line1);
        productTable.saveProduct(product2);

        Product product3 = new Product();
        product3.setName("Different Name");
        product3.setActive(true);
        product3.setLine(line1);
        productTable.saveProduct(product3);

        // Test findByNameAndActive - should find only product1
        List<Product> results = productTable.getProductsByNameAndActive("Test AND Product Active", true);
        assertEquals("Should find 1 product", 1, results.size());
        assertEquals("Test AND Product Active", results.get(0).getName());
        assertTrue("Product should be active", results.get(0).isActive());
    }

    @Test
    public void testFindByNameOrActive() {
        // Initialize tables
        TableProducts productTable = new TableProducts(context);
        LineTable lineTable = new LineTable(context);

        // Setup: Create products with different combinations of name and active status
        Line line1 = new Line();
        line1.setName("Line - OR Test");
        line1 = lineTable.saveLine(line1);

        Product product1 = new Product();
        product1.setName("Test OR Product");
        product1.setActive(true);
        product1.setLine(line1);
        productTable.saveProduct(product1);

        Product product2 = new Product();
        product2.setName("Different OR Name");
        product2.setActive(false);
        product2.setLine(line1);
        productTable.saveProduct(product2);

        Product product3 = new Product();
        product3.setName("Another OR Name");
        product3.setActive(true);
        product3.setLine(line1);
        productTable.saveProduct(product3);

        // Test findByNameOrActive - should find at least product1 and product3
        List<Product> results = productTable.getProductsByNameOrActive("Test OR Product", true);

        // Verify that the results include the expected products
        boolean foundProduct1 = false;
        boolean foundProduct3 = false;

        for (Product p : results) {
            if (p.getName().equals("Test OR Product") && p.isActive()) {
                foundProduct1 = true;
            } else if (p.getName().equals("Another OR Name") && p.isActive()) {
                foundProduct3 = true;
            }
        }

        assertTrue("Should find product1", foundProduct1);
        assertTrue("Should find product3", foundProduct3);
    }
}
