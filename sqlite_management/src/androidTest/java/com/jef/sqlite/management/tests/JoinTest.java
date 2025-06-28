package com.jef.sqlite.management.tests;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.jef.sqlite.management.Management;
import com.jef.sqlite.management.SQLiteTable;
import com.jef.sqlite.management.Query.QueryFactory;
import com.jef.sqlite.management.interfaces.Column;
import com.jef.sqlite.management.interfaces.Join;
import com.jef.sqlite.management.interfaces.Table;
import com.jef.sqlite.management.models.Line;
import com.jef.sqlite.management.tables.LineTable;
import com.jef.sqlite.management.tables.TableProducts;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

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
        // Create a ProductWithCategory table
        ProductWithCategoryTable productWithCategoryTable = new ProductWithCategoryTable(appContext);

        // Get products with category
        List<ProductWithCategory> productsWithCategory = productWithCategoryTable.getAllProductsWithCategory();

        // Verify that the join worked
        assertFalse("Should find at least one product", productsWithCategory.isEmpty());

        ProductWithCategory productWithCategory = productsWithCategory.get(0);
        System.out.println("[DEBUG_LOG] Product ID: " + productWithCategory.getId());
        System.out.println("[DEBUG_LOG] Product Name: " + productWithCategory.getName());
        System.out.println("[DEBUG_LOG] Product Line: " + productWithCategory.getLine());
        System.out.println("[DEBUG_LOG] Product Category ID: " + productWithCategory.getCategoryId());

        assertNotNull("Product should not be null", productWithCategory);
        assertNotNull("Category should not be null", productWithCategory.getCategory());

        if (productWithCategory.getCategory() != null) {
            System.out.println("[DEBUG_LOG] Category ID: " + productWithCategory.getCategory().getId());
            System.out.println("[DEBUG_LOG] Category Name: " + productWithCategory.getCategory().getName());
        } else {
            System.out.println("[DEBUG_LOG] Category is null");
        }

        assertEquals("Category name should match", "Test Category", productWithCategory.getCategory().getName());

        System.out.println("Successfully tested join relationship between Product and Category");
    }

    // Entity classes for testing
    @Table(name = "categories")
    public static class Category {
        @Column(name = "id", isPrimaryKey = true)
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
        @Column(name = "id", isPrimaryKey = true, isAutoIncrement = true)
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
    public interface CategoryQuery {
        List<Category> findAll();
        Category save(Category category);
    }

    public interface ProductWithCategoryQuery {
        List<ProductWithCategory> findAll();
        ProductWithCategory save(ProductWithCategory product);
    }

    // Table classes
    public static class CategoryTable extends SQLiteTable<Category> {
        public CategoryTable(Context context) {
            super(new Management(context));
        }

        private CategoryQuery query() {
            return QueryFactory.create(CategoryQuery.class, Category.class, getManagement());
        }

        public List<Category> getAllCategories() {
            return query().findAll();
        }

        public Category saveCategory(Category category) {
            return query().save(category);
        }
    }

    public static class ProductWithCategoryTable extends SQLiteTable<ProductWithCategory> {
        public ProductWithCategoryTable(Context context) {
            super(new Management(context));
        }

        private ProductWithCategoryQuery query() {
            return QueryFactory.create(ProductWithCategoryQuery.class, ProductWithCategory.class, getManagement());
        }

        public List<ProductWithCategory> getAllProductsWithCategory() {
            return query().findAll();
        }

        public ProductWithCategory saveProductWithCategory(ProductWithCategory product) {
            return query().save(product);
        }
    }
}