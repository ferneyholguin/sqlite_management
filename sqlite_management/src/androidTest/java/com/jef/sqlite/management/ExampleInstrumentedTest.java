package com.jef.sqlite.management;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.jef.sqlite.management.models.Line;
import com.jef.sqlite.management.models.Product;
import com.jef.sqlite.management.tables.LineTable;
import com.jef.sqlite.management.tables.TableProducts;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    @Test
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("com.jef.sqlite.management.test", appContext.getPackageName());

        // Test the existing query functionality
        TableProducts tableProducts = new TableProducts(appContext);
        List<Product> products = tableProducts.getAllProducts();

        // Print the number of products found
        System.out.println("Found " + products.size() + " products");
    }

    @Test
    public void testSaveProduct() {
        // Get the context
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

        // Create a LineTable instance to ensure the Line table is created
        LineTable lineTable = new LineTable(appContext);

        // Create and save a Line object
        Line line = new Line();
        line.setName("Test Line");
        Line savedLine = lineTable.saveLine(line);

        // Create a table products instance
        TableProducts tableProducts = new TableProducts(appContext);

        // Create a new product with a unique name using timestamp
        Product product = new Product();
        String uniqueName = "Test Product " + System.currentTimeMillis();
        product.setName(uniqueName);

        // Set the saved Line object on the product
        product.setLine(savedLine);

        // Save the product
        Product savedProduct = tableProducts.saveProduct(product);

        // Verify that the product was saved and has an ID
        assertNotNull("Saved product should not be null", savedProduct);
        assertTrue("Saved product should have an ID greater than 0", savedProduct.getId() > 0);
        assertEquals("Saved product should have the same name", uniqueName, savedProduct.getName());
        assertNotNull("Saved product should have a line", savedProduct.getLine());
        assertEquals("Saved product should have the same line name", "Test Line", savedProduct.getLine().getName());

        // Print the saved product ID
        System.out.println("Saved product with ID: " + savedProduct.getId());

        // Retrieve all products
        List<Product> products = tableProducts.getAllProducts();

        // Verify that the product was retrieved
        assertFalse("Should find at least one product", products.isEmpty());

        // Find the product with the same ID as the saved product
        Product retrievedProduct = null;
        for (Product p : products) {
            if (p.getId() == savedProduct.getId()) {
                retrievedProduct = p;
                break;
            }
        }

        assertNotNull("Should find the saved product", retrievedProduct);

        // Update the product using the update methods instead of save
        Line updatedLine = new Line();
        updatedLine.setName("Updated Line");
        Line savedUpdatedLine = lineTable.saveLine(updatedLine);

        // Update the product's line ID using the update method
        tableProducts.updateProductNameById("Updated " + uniqueName, savedProduct.getId());

        // Retrieve the updated product
        java.util.Optional<Product> updatedProductOpt = tableProducts.getProductById(savedProduct.getId());
        assertTrue("Updated product should be present", updatedProductOpt.isPresent());
        Product updatedProduct = updatedProductOpt.get();

        // Verify that the product was updated
        assertEquals("Updated product should have the same ID", savedProduct.getId(), updatedProduct.getId());
        assertEquals("Updated product should have the updated name", "Updated " + uniqueName, updatedProduct.getName());

        // Retrieve all products again
        products = tableProducts.getAllProducts();

        // Verify that the product was updated in the database
        assertFalse("Should find at least one product", products.isEmpty());

        // Find the product with the same ID as the updated product
        Product retrievedUpdatedProduct = null;
        for (Product p : products) {
            if (p.getId() == updatedProduct.getId()) {
                retrievedUpdatedProduct = p;
                break;
            }
        }

        assertNotNull("Should find the updated product", retrievedUpdatedProduct);
        assertEquals("Should find the updated product with updated name", "Updated " + uniqueName, retrievedUpdatedProduct.getName());

        // Print success message
        System.out.println("Successfully tested save functionality for new and existing products");
    }
}
