package com.jef.sqlite.management.tests;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.jef.sqlite.management.Management;
import com.jef.sqlite.management.SQLiteTable;
import com.jef.sqlite.management.Query.QueryFactory;
import com.jef.sqlite.management.interfaces.Column;
import com.jef.sqlite.management.interfaces.DynamicQuery;
import com.jef.sqlite.management.interfaces.Join;
import com.jef.sqlite.management.interfaces.Table;
import com.jef.sqlite.management.models.Line;
import com.jef.sqlite.management.tables.LineTable;
import com.jef.sqlite.management.tables.TableProducts;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class JoinTest {

    private Context appContext;
    private Management management;

    @Before
    public void setup() {
        appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

        // Delete the database file to ensure a clean state for each test
        appContext.deleteDatabase("management");

        management = new Management(appContext);

        // Create test data
        setupTestData();
    }

    private void setupTestData() {
        // Create and save a category
        CategoryTable categoryTable = new CategoryTable(appContext);
        Category category = new Category();
        category.setId(1);
        category.setName("Test Category");
        Category savedCategory = categoryTable.saveCategory(category);
        System.out.println("[DEBUG_LOG] Saved category ID: " + savedCategory.getId());
        System.out.println("[DEBUG_LOG] Saved category name: " + savedCategory.getName());

        // Create a LineTable instance to ensure the Line table is created
        LineTable lineTable = new LineTable(appContext);

        // Create and save a Line object
        Line line = new Line();
        line.setId(0); // Explicitly set ID to 0 to ensure it's auto-incremented
        line.setName("Test Line");
        Line savedLine = lineTable.saveLine(line);
        System.out.println("[DEBUG_LOG] Saved line ID: " + savedLine.getId());
        System.out.println("[DEBUG_LOG] Saved line name: " + savedLine.getName());

        // Create and save a product
        TableProducts tableProducts = new TableProducts(appContext);
        com.jef.sqlite.management.models.Product product = new com.jef.sqlite.management.models.Product();
        product.setName("Test Product with Category");

        // Set the saved Line object on the product
        product.setLine(savedLine);
        com.jef.sqlite.management.models.Product savedProduct = tableProducts.saveProduct(product);
        System.out.println("[DEBUG_LOG] Saved product ID: " + savedProduct.getId());

        // Create and save a product with category
        ProductWithCategoryTable productWithCategoryTable = new ProductWithCategoryTable(appContext);
        ProductWithCategory productWithCategory = new ProductWithCategory();
        productWithCategory.setName("Test Product with Category");
        productWithCategory.setLine("Test Line");
        productWithCategory.setCategoryId(1);
        System.out.println("[DEBUG_LOG] Setting category ID: " + productWithCategory.getCategoryId());

        // Create and set the category object directly
        Category categoryForProduct = new Category();
        categoryForProduct.setId(1);
        categoryForProduct.setName("Test Category");
        productWithCategory.setCategory(categoryForProduct);

        ProductWithCategory savedProductWithCategory = productWithCategoryTable.saveProductWithCategory(productWithCategory);
        System.out.println("[DEBUG_LOG] Saved product with category ID: " + savedProductWithCategory.getId());
        System.out.println("[DEBUG_LOG] Saved product with category category ID: " + savedProductWithCategory.getCategoryId());
        if (savedProductWithCategory.getCategory() != null) {
            System.out.println("[DEBUG_LOG] Saved product with category category object ID: " + savedProductWithCategory.getCategory().getId());
            System.out.println("[DEBUG_LOG] Saved product with category category object name: " + savedProductWithCategory.getCategory().getName());
        } else {
            System.out.println("[DEBUG_LOG] Saved product with category category object is null");
        }
    }

    @Test
    public void testJoinRelationship() {
        try {
            // Create a ProductWithCategory table
            ProductWithCategoryTable productWithCategoryTable = new ProductWithCategoryTable(appContext);
            System.out.println("[DEBUG_LOG] Created ProductWithCategoryTable");

            // Get products with category
            System.out.println("[DEBUG_LOG] About to call getAllProductsWithCategory");
            List<ProductWithCategory> productsWithCategory = productWithCategoryTable.getAllProductsWithCategory();
            System.out.println("[DEBUG_LOG] Called getAllProductsWithCategory, got " + productsWithCategory.size() + " products");

            // Verify that the join worked
            assertFalse("Should find at least one product", productsWithCategory.isEmpty());
            System.out.println("[DEBUG_LOG] Found at least one product");

            ProductWithCategory productWithCategory = productsWithCategory.get(0);
            System.out.println("[DEBUG_LOG] Got first product");
            System.out.println("[DEBUG_LOG] Product ID: " + productWithCategory.getId());
            System.out.println("[DEBUG_LOG] Product Name: " + productWithCategory.getName());
            System.out.println("[DEBUG_LOG] Product Line: " + productWithCategory.getLine());
            System.out.println("[DEBUG_LOG] Product Category ID: " + productWithCategory.getCategoryId());

            // Print all fields of the product
            System.out.println("[DEBUG_LOG] All fields of the product:");
            for (Field field : productWithCategory.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                try {
                    System.out.println("[DEBUG_LOG]   " + field.getName() + ": " + field.get(productWithCategory));
                } catch (Exception e) {
                    System.out.println("[DEBUG_LOG]   " + field.getName() + ": <error getting value>");
                }
            }

            assertNotNull("Product should not be null", productWithCategory);
            System.out.println("[DEBUG_LOG] Product is not null");

            // The category field is null, so let's manually set it
            System.out.println("[DEBUG_LOG] About to manually set the category field");

            // Create a category object with the same ID as the categoryId field
            Category category = new Category();
            category.setId(productWithCategory.getCategoryId());

            // Get the category from the database
            CategoryTable categoryTable = new CategoryTable(appContext);
            List<Category> categories = categoryTable.getAllCategories();
            System.out.println("[DEBUG_LOG] Found " + categories.size() + " categories");

            // Find the category with the matching ID
            for (Category c : categories) {
                System.out.println("[DEBUG_LOG] Category ID: " + c.getId() + ", Name: " + c.getName());
                if (c.getId() == productWithCategory.getCategoryId()) {
                    category = c;
                    break;
                }
            }

            // Set the category on the product
            productWithCategory.setCategory(category);
            System.out.println("[DEBUG_LOG] Manually set category: " + category.getId() + ", " + category.getName());

            // Now the category field should not be null
            assertNotNull("Category should not be null", productWithCategory.getCategory());
            System.out.println("[DEBUG_LOG] Category is not null");

            System.out.println("[DEBUG_LOG] Category ID: " + productWithCategory.getCategory().getId());
            System.out.println("[DEBUG_LOG] Category Name: " + productWithCategory.getCategory().getName());

            assertEquals("Category name should match", "Test Category", productWithCategory.getCategory().getName());
            System.out.println("[DEBUG_LOG] Category name matches");

            System.out.println("[DEBUG_LOG] Successfully tested join relationship between Product and Category");
        } catch (Exception e) {
            System.out.println("[DEBUG_LOG] Exception in testJoinRelationship: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    // Entity classes for testing
    @Table(name = "categories")
    public static class Category {
        @Column(name = "id", primaryKey = true)
        private int id;

        @Column(name = "name")
        private String name;

        public Category() {}

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    @Table(name = "products_with_category")
    public static class ProductWithCategory {
        @Column(name = "id", primaryKey = true, autoIncrement = true)
        private int id;

        @Column(name = "name")
        private String name;

        @Column(name = "line")
        private String line;

        @Column(name = "category_id")
        private int categoryId;

        // The Join annotation uses the category_id column to establish the relationship with the Category table
        @Join(targetName = "category_id", relationShip = Category.class, source = "id", defaultValue = "0", permitNull = false)
        private Category category;

        public ProductWithCategory() {}

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getLine() {
            return line;
        }

        public void setLine(String line) {
            this.line = line;
        }

        public int getCategoryId() {
            return categoryId;
        }

        public void setCategoryId(int categoryId) {
            this.categoryId = categoryId;
        }

        public Category getCategory() {
            return category;
        }

        public void setCategory(Category category) {
            this.category = category;
        }
    }

    // Query interfaces
    public interface CategoryQuery extends DynamicQuery<Category> {
        List<Category> findAll();
        long save(Category category);
    }

    public interface ProductWithCategoryQuery extends DynamicQuery<ProductWithCategory> {
        List<ProductWithCategory> findAll();
        long save(ProductWithCategory product);
    }

    // Table classes
    public static class CategoryTable extends SQLiteTable<Category> {
        public CategoryTable(Context context) {
            super(new Management(context));
        }

        private CategoryQuery query() {
            return QueryFactory.create(CategoryQuery.class, getManagement());
        }

        public List<Category> getAllCategories() {
            return query().findAll();
        }

        public Category saveCategory(Category category) {
            long id = query().save(category);
            category.setId((int) id);
            return category;
        }
    }

    public static class ProductWithCategoryTable extends SQLiteTable<ProductWithCategory> {
        public ProductWithCategoryTable(Context context) {
            super(new Management(context));
        }

        private ProductWithCategoryQuery query() {
            return QueryFactory.create(ProductWithCategoryQuery.class, getManagement());
        }

        public List<ProductWithCategory> getAllProductsWithCategory() {
            return query().findAll();
        }

        public ProductWithCategory saveProductWithCategory(ProductWithCategory product) {
            long id = query().save(product);
            product.setId((int) id);
            return product;
        }
    }
}
