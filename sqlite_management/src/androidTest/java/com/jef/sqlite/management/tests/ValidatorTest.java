package com.jef.sqlite.management.tests;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.jef.sqlite.management.Management;
import com.jef.sqlite.management.Query.QueryInvocation.QueryValidatorHandler;
import com.jef.sqlite.management.exceptions.SQLiteException;
import com.jef.sqlite.management.models.Product;
import com.jef.sqlite.management.models.Line;
import com.jef.sqlite.management.tables.LineTable;
import com.jef.sqlite.management.tables.TableProducts;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Test class for the QueryValidatorHandler.
 * This test demonstrates how to use the validator to check if entities are valid for database insertion.
 */
@RunWith(AndroidJUnit4.class)
public class ValidatorTest {

    private Context context;
    private TableProducts productTable;
    private LineTable lineTable;
    private QueryValidatorHandler<Product> productValidator;
    private QueryValidatorHandler<Line> lineValidator;

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

        // Create validators
        System.out.println("[DEBUG_LOG] Creating validators");
        Management management = new Management(context);
        productValidator = new QueryValidatorHandler<>(Product.class, management);
        lineValidator = new QueryValidatorHandler<>(Line.class, management);

        // Setup test data
        System.out.println("[DEBUG_LOG] Setting up test data");
        setupTestData();
        System.out.println("[DEBUG_LOG] Setup complete");
    }

    private void setupTestData() {
        try {
            System.out.println("[DEBUG_LOG] Starting setupTestData");

            // Create a line
            System.out.println("[DEBUG_LOG] Creating Line 1");
            Line line1 = new Line();
            line1.setName("Line 1");
            line1 = lineTable.saveLine(line1);
            System.out.println("[DEBUG_LOG] Saved Line 1 with ID: " + line1.getId());

            // Create a product with a unique name for each test
            String productName = "Product " + System.currentTimeMillis();
            System.out.println("[DEBUG_LOG] Creating product with name: " + productName);
            Product product1 = new Product();
            product1.setName(productName);
            product1.setLine(line1);
            product1.setActive(true);
            Product savedProduct1 = productTable.saveProduct(product1);
            System.out.println("[DEBUG_LOG] Saved product with ID: " + savedProduct1.getId());

            System.out.println("[DEBUG_LOG] setupTestData complete");
        } catch (Exception e) {
            System.out.println("[DEBUG_LOG] Exception in setupTestData: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Test
    public void testValidEntity() {
        System.out.println("[DEBUG_LOG] Starting testValidEntity");
        try {
            // Create a valid product
            Product product = new Product();
            product.setName("New Valid Product");
            product.setActive(true);

            // Get a line to set in the product
            Line line = lineTable.getAllLines().get(0);
            product.setLine(line);

            // Validate the product
            boolean isValid = productValidator.validateEntity(product);

            // Verify the result
            assertTrue("Valid product should pass validation", isValid);

            System.out.println("[DEBUG_LOG] testValidEntity passed");
        } catch (Exception e) {
            System.out.println("[DEBUG_LOG] Exception in testValidEntity: " + e.getMessage());
            e.printStackTrace();
            fail("Exception in testValidEntity: " + e.getMessage());
        }
    }

    @Test
    public void testNullValueInNonNullField() {
        System.out.println("[DEBUG_LOG] Starting testNullValueInNonNullField");
        try {
            // Create a product with null name (assuming name is non-null)
            Product product = new Product();
            product.setName(null); // Name should be non-null
            product.setActive(true);

            // Get a line to set in the product
            Line line = lineTable.getAllLines().get(0);
            product.setLine(line);

            // Validate the product - should throw exception
            productValidator.validateEntity(product);

            // If we get here, validation didn't throw an exception
            fail("Validation should fail for null value in non-null field");
        } catch (SQLiteException e) {
            // Expected exception
            System.out.println("[DEBUG_LOG] Expected exception: " + e.getMessage());
            assertTrue("Exception message should mention null field", 
                    e.getMessage().contains("cannot be null"));
            System.out.println("[DEBUG_LOG] testNullValueInNonNullField passed");
        } catch (Exception e) {
            System.out.println("[DEBUG_LOG] Unexpected exception: " + e.getMessage());
            e.printStackTrace();
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    @Test
    public void testDuplicateValueInUniqueField() {
        System.out.println("[DEBUG_LOG] Starting testDuplicateValueInUniqueField");
        try {
            // Get the existing product name from the database
            Product existingProduct = productTable.getAllProducts().get(0);
            String existingName = existingProduct.getName();
            System.out.println("[DEBUG_LOG] Found existing product with name: " + existingName);

            // Create a product with the same name as an existing product
            Product product = new Product();
            product.setName(existingName); // Use the existing name to trigger the unique constraint
            product.setActive(false);

            // Get a line to set in the product
            Line line = lineTable.getAllLines().get(0);
            product.setLine(line);

            // Validate the product - should throw exception
            productValidator.validateEntity(product);

            // If we get here, validation didn't throw an exception
            fail("Validation should fail for duplicate value in unique field");
        } catch (SQLiteException e) {
            // Expected exception
            System.out.println("[DEBUG_LOG] Expected exception: " + e.getMessage());
            assertTrue("Exception message should mention unique field", 
                    e.getMessage().contains("must be unique"));
            System.out.println("[DEBUG_LOG] testDuplicateValueInUniqueField passed");
        } catch (Exception e) {
            System.out.println("[DEBUG_LOG] Unexpected exception: " + e.getMessage());
            e.printStackTrace();
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    @Test
    public void testNullValueInJoinField() {
        System.out.println("[DEBUG_LOG] Starting testNullValueInJoinField");
        try {
            // Create a product with a null line (Join field)
            Product product = new Product();
            product.setName("Product with Null Line");
            product.setActive(true);
            product.setLine(null); // Line is a Join field and should not be null by default

            // Validate the product - should throw exception
            productValidator.validateEntity(product);

            // If we get here, validation didn't throw an exception
            fail("Validation should fail for null value in Join field");
        } catch (SQLiteException e) {
            // Expected exception
            System.out.println("[DEBUG_LOG] Expected exception: " + e.getMessage());
            assertTrue("Exception message should mention Join annotation", 
                    e.getMessage().contains("with Join annotation cannot be null"));
            System.out.println("[DEBUG_LOG] testNullValueInJoinField passed");
        } catch (Exception e) {
            System.out.println("[DEBUG_LOG] Unexpected exception: " + e.getMessage());
            e.printStackTrace();
            fail("Unexpected exception: " + e.getMessage());
        }
    }
    @Test
    public void testValidateValidProduct() {
        System.out.println("[DEBUG_LOG] Starting testValidateValidProduct");
        try {
            // Create a valid product
            Product product = new Product();
            product.setName("New Valid Product for Interface Test");
            product.setActive(true);

            // Get a line to set in the product
            Line line = lineTable.getAllLines().get(0);
            product.setLine(line);

            // Validate the product using the interface method
            boolean isValid = productTable.validateProduct(product);

            // Verify the result
            assertTrue("Valid product should pass validation through interface", isValid);

            System.out.println("[DEBUG_LOG] testValidateValidProduct passed");
        } catch (Exception e) {
            System.out.println("[DEBUG_LOG] Exception in testValidateValidProduct: " + e.getMessage());
            e.printStackTrace();
            fail("Exception in testValidateValidProduct: " + e.getMessage());
        }
    }

    @Test
    public void testValidateNullValueInNonNullField() {
        System.out.println("[DEBUG_LOG] Starting testValidateNullValueInNonNullField");
        try {
            // Create a product with null name (assuming name is non-null)
            Product product = new Product();
            product.setName(null); // Name should be non-null
            product.setActive(true);

            // Get a line to set in the product
            Line line = lineTable.getAllLines().get(0);
            product.setLine(line);

            // Validate the product - should throw exception
            productTable.validateProduct(product);

            // If we get here, validation didn't throw an exception
            fail("Validation through interface should fail for null value in non-null field");
        } catch (SQLiteException e) {
            // Expected exception
            System.out.println("[DEBUG_LOG] Expected exception: " + e.getMessage());
            assertTrue("Exception message should mention null field", 
                    e.getMessage().contains("cannot be null"));
            System.out.println("[DEBUG_LOG] testValidateNullValueInNonNullField passed");
        } catch (Exception e) {
            System.out.println("[DEBUG_LOG] Unexpected exception: " + e.getMessage());
            e.printStackTrace();
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    @Test
    public void testValidateDuplicateValueInUniqueField() {
        System.out.println("[DEBUG_LOG] Starting testValidateDuplicateValueInUniqueField");
        try {
            // Get the existing product name from the database
            Product existingProduct = productTable.getAllProducts().get(0);
            String existingName = existingProduct.getName();
            System.out.println("[DEBUG_LOG] Found existing product with name: " + existingName);

            // Create a product with the same name as an existing product
            Product product = new Product();
            product.setName(existingName); // Use the existing name to trigger the unique constraint
            product.setActive(false);

            // Get a line to set in the product
            Line line = lineTable.getAllLines().get(0);
            product.setLine(line);

            // Validate the product - should throw exception
            productTable.validateProduct(product);

            // If we get here, validation didn't throw an exception
            fail("Validation through interface should fail for duplicate value in unique field");
        } catch (SQLiteException e) {
            // Expected exception
            System.out.println("[DEBUG_LOG] Expected exception: " + e.getMessage());
            assertTrue("Exception message should mention unique field", 
                    e.getMessage().contains("must be unique"));
            System.out.println("[DEBUG_LOG] testValidateDuplicateValueInUniqueField passed");
        } catch (Exception e) {
            System.out.println("[DEBUG_LOG] Unexpected exception: " + e.getMessage());
            e.printStackTrace();
            fail("Unexpected exception: " + e.getMessage());
        }
    }
}
