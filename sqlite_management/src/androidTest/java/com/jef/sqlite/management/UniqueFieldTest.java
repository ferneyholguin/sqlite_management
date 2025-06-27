package com.jef.sqlite.management;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.jef.sqlite.management.interfaces.Column;
import com.jef.sqlite.management.interfaces.Join;
import com.jef.sqlite.management.interfaces.Table;
import com.jef.sqlite.management.Query.QueryFactory;
import com.jef.sqlite.management.exceptions.SQLiteException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class UniqueFieldTest {

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
        // Create and save a category
        CategoryTable categoryTable = new CategoryTable(appContext);

        // Check if the category already exists
        List<Category> existingCategories = categoryTable.getAllCategories();
        Category savedCategory;

        if (!existingCategories.isEmpty()) {
            // Use the existing category
            savedCategory = existingCategories.get(0);
            System.out.println("[DEBUG_LOG] Using existing category ID: " + savedCategory.getId());
            System.out.println("[DEBUG_LOG] Using existing category name: " + savedCategory.getName());
        } else {
            // Create a new category
            Category category = new Category();
            category.setName("Test Category");
            savedCategory = categoryTable.saveCategory(category);
            System.out.println("[DEBUG_LOG] Saved new category ID: " + savedCategory.getId());
            System.out.println("[DEBUG_LOG] Saved new category name: " + savedCategory.getName());
        }

        // Create and save a product with unique code
        ProductWithUniqueCodeTable productTable = new ProductWithUniqueCodeTable(appContext);

        // Check if a product with this code already exists
        List<ProductWithUniqueCode> existingProducts = productTable.getAllProducts();
        boolean productExists = false;

        for (ProductWithUniqueCode p : existingProducts) {
            if ("UNIQUE001".equals(p.getCode())) {
                productExists = true;
                System.out.println("[DEBUG_LOG] Product with code UNIQUE001 already exists");
                break;
            }
        }

        if (!productExists) {
            ProductWithUniqueCode product = new ProductWithUniqueCode();
            product.setName("Test Product");
            product.setCode("UNIQUE001");
            product.setCategoryId(savedCategory.getId());

            // Set the category object directly
            product.setCategory(savedCategory);

            ProductWithUniqueCode savedProduct = productTable.saveProduct(product);
            System.out.println("[DEBUG_LOG] Saved product ID: " + savedProduct.getId());
            System.out.println("[DEBUG_LOG] Saved product name: " + savedProduct.getName());
            System.out.println("[DEBUG_LOG] Saved product code: " + savedProduct.getCode());
        }
    }

    @Test
    public void testUniqueColumnConstraint() {
        // Create a product table
        ProductWithUniqueCodeTable productTable = new ProductWithUniqueCodeTable(appContext);

        // Create a product with the same unique code as an existing product
        ProductWithUniqueCode product = new ProductWithUniqueCode();
        product.setName("Another Product");
        product.setCode("UNIQUE001"); // This code already exists

        // Try to save the product - should throw an exception due to unique constraint
        try {
            productTable.saveProduct(product);
            fail("Expected SQLiteException due to unique constraint violation");
        } catch (SQLiteException e) {
            // Expected exception
            System.out.println("[DEBUG_LOG] Caught expected exception: " + e.getMessage());
            assertTrue(e.getMessage().contains("UNIQUE constraint failed") || 
                       e.getMessage().contains("column code is not unique"));
        }
    }

    @Test
    public void testUniqueJoinConstraint() {
        // Create a category table
        CategoryTable categoryTable = new CategoryTable(appContext);

        // Get the existing category
        List<Category> categories = categoryTable.getAllCategories();
        assertFalse("Should find at least one category", categories.isEmpty());
        Category existingCategory = categories.get(0);

        // Create a product table
        ProductWithUniqueCategoryTable productTable = new ProductWithUniqueCategoryTable(appContext);

        // Create a product with a unique category
        ProductWithUniqueCategory product1 = new ProductWithUniqueCategory();
        product1.setName("Product with Unique Category");
        product1.setCategoryId(existingCategory.getId());
        product1.setCategory(existingCategory);

        // Save the first product
        ProductWithUniqueCategory savedProduct1 = productTable.saveProduct(product1);
        System.out.println("[DEBUG_LOG] Saved product 1 ID: " + savedProduct1.getId());

        // Create another product with the same category
        ProductWithUniqueCategory product2 = new ProductWithUniqueCategory();
        product2.setName("Another Product with Same Category");
        product2.setCategoryId(existingCategory.getId());
        product2.setCategory(existingCategory);

        // Try to save the second product - should throw an exception due to unique constraint
        try {
            productTable.saveProduct(product2);
            fail("Expected SQLiteException due to unique join constraint violation");
        } catch (SQLiteException e) {
            // Expected exception
            System.out.println("[DEBUG_LOG] Caught expected exception: " + e.getMessage());
            assertTrue(e.getMessage().contains("UNIQUE constraint failed") || 
                       e.getMessage().contains("column category_id is not unique"));
        }
    }

    // Entity classes for testing
    @Table(name = "categories_unique")
    public static class Category {
        @Column(name = "id", isPrimaryKey = true, isAutoIncrement = true)
        private int id;

        @Column(name = "name", isUnique = true)
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

    @Table(name = "products_with_unique_code")
    public static class ProductWithUniqueCode {
        @Column(name = "id", isPrimaryKey = true, isAutoIncrement = true)
        private int id;

        @Column(name = "name")
        private String name;

        @Column(name = "code", isUnique = true)
        private String code;

        @Column(name = "category_id")
        private int categoryId;

        @Join(targetName = "id", relationShip = Category.class, source = "category_id")
        private Category category;

        public ProductWithUniqueCode() {}

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

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
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

    @Table(name = "products_with_unique_category")
    public static class ProductWithUniqueCategory {
        @Column(name = "id", isPrimaryKey = true, isAutoIncrement = true)
        private int id;

        @Column(name = "name")
        private String name;

        @Column(name = "category_id")
        private int categoryId;

        @Join(targetName = "id", relationShip = Category.class, source = "category_id", isUnique = true)
        private Category category;

        public ProductWithUniqueCategory() {}

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

    // Table classes for testing
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

    public static class ProductWithUniqueCodeTable extends SQLiteTable<ProductWithUniqueCode> {
        public ProductWithUniqueCodeTable(Context context) {
            super(new Management(context));
        }

        private ProductWithUniqueCodeQuery query() {
            return QueryFactory.create(ProductWithUniqueCodeQuery.class, ProductWithUniqueCode.class, getManagement());
        }

        public List<ProductWithUniqueCode> getAllProducts() {
            return query().findAll();
        }

        public ProductWithUniqueCode saveProduct(ProductWithUniqueCode product) {
            return query().save(product);
        }
    }

    public static class ProductWithUniqueCategoryTable extends SQLiteTable<ProductWithUniqueCategory> {
        public ProductWithUniqueCategoryTable(Context context) {
            super(new Management(context));
        }

        private ProductWithUniqueCategoryQuery query() {
            return QueryFactory.create(ProductWithUniqueCategoryQuery.class, ProductWithUniqueCategory.class, getManagement());
        }

        public List<ProductWithUniqueCategory> getAllProducts() {
            return query().findAll();
        }

        public ProductWithUniqueCategory saveProduct(ProductWithUniqueCategory product) {
            return query().save(product);
        }
    }

    // Query interfaces
    public interface CategoryQuery {
        List<Category> findAll();
        Category save(Category category);
    }

    public interface ProductWithUniqueCodeQuery {
        List<ProductWithUniqueCode> findAll();
        ProductWithUniqueCode save(ProductWithUniqueCode product);
    }

    public interface ProductWithUniqueCategoryQuery {
        List<ProductWithUniqueCategory> findAll();
        ProductWithUniqueCategory save(ProductWithUniqueCategory product);
    }
}
