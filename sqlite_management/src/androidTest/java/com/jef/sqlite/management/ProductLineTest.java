package com.jef.sqlite.management;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.jef.sqlite.management.Query.QueryFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Test class for testing the Product class with a Line object.
 */
@RunWith(AndroidJUnit4.class)
public class ProductLineTest {

    private Context appContext;
    private Management management;

    @Before
    public void setup() {
        appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        management = new Management(appContext);

        // Create test data
        setupTestData();
    }

    private void setupTestData() {
        // Create and save a line
        LineTable lineTable = new LineTable(appContext);
        Line line = new Line();
        line.setId(1);
        line.setName("Test Line");
        Line savedLine = lineTable.saveLine(line);
        System.out.println("[DEBUG_LOG] Saved line ID: " + savedLine.getId());
        System.out.println("[DEBUG_LOG] Saved line name: " + savedLine.getName());

        // Create and save a product with line
        TableProducts tableProducts = new TableProducts(appContext);
        Product product = new Product();
        product.setName("Test Product with Line");

        // Create and set the line object directly
        Line lineForProduct = new Line();
        lineForProduct.setId(1);
        lineForProduct.setName("Test Line");
        product.setLine(lineForProduct);

        Product savedProduct = tableProducts.saveProduct(product);
        System.out.println("[DEBUG_LOG] Saved product ID: " + savedProduct.getId());
        System.out.println("[DEBUG_LOG] Saved product name: " + savedProduct.getName());
        if (savedProduct.getLine() != null) {
            System.out.println("[DEBUG_LOG] Saved product line ID: " + savedProduct.getLine().getId());
            System.out.println("[DEBUG_LOG] Saved product line name: " + savedProduct.getLine().getName());
        } else {
            System.out.println("[DEBUG_LOG] Saved product line is null");
        }
    }

    @Test
    public void testProductLineRelationship() {
        // Create a TableProducts instance
        TableProducts tableProducts = new TableProducts(appContext);

        // Get all products
        List<Product> products = tableProducts.getAllProducts();

        // Verify that the join worked
        assertFalse("Should find at least one product", products.isEmpty());

        Product product = products.get(0);
        System.out.println("[DEBUG_LOG] Product ID: " + product.getId());
        System.out.println("[DEBUG_LOG] Product Name: " + product.getName());

        assertNotNull("Product should not be null", product);
        assertNotNull("Line should not be null", product.getLine());

        if (product.getLine() != null) {
            System.out.println("[DEBUG_LOG] Line ID: " + product.getLine().getId());
            System.out.println("[DEBUG_LOG] Line Name: " + product.getLine().getName());
        } else {
            System.out.println("[DEBUG_LOG] Line is null");
        }

        assertEquals("Line name should match", "Test Line", product.getLine().getName());

        System.out.println("Successfully tested join relationship between Product and Line");
    }

    /**
     * Table class for Line entity
     */
    public static class LineTable extends SQLiteTable<Line> {
        public LineTable(Context context) {
            super(new Management(context));
        }

        private LineQuery query() {
            return QueryFactory.create(LineQuery.class, Line.class, getManagement());
        }

        public List<Line> getAllLines() {
            return query().findAll();
        }

        public Line saveLine(Line line) {
            return query().save(line);
        }
    }

    /**
     * Query interface for Line entity
     */
    public interface LineQuery {
        List<Line> findAll();
        Line save(Line line);
    }
}
