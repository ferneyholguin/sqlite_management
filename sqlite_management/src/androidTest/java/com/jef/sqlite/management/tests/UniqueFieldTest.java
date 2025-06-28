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
        Category category = new Category();
        category.setId(1);
        category.setName("Test Category");
        Category savedCategory = categoryTable.saveCategory(category);
        System.out.println("[DEBUG_LOG] Saved category ID: " + savedCategory.getId());
        System.out.println("[DEBUG_LOG] Saved category name: " + savedCategory.getName());

        // Create and save a product with unique code
        ProductWithUniqueCodeTable productTable = new ProductWithUniqueCodeTable(appContext);
        ProductWithUniqueCode product = new ProductWithUniqueCode();
        product.setName("Test Product");
        product.setCode("UNIQUE123");
        product.setCategoryId(1);

        // Create and set the category object directly
        Category categoryForProduct = new Category();
        categoryForProduct.setId(1);
        categoryForProduct.setName("Test Category");
        product.setCategory(categoryForProduct);

        ProductWithUniqueCode savedProduct = productTable.saveProduct(product);
        System.out.println("[DEBUG_LOG] Saved product ID: " + savedProduct.getId());
        System.out.println("[DEBUG_LOG] Saved product name: " + savedProduct.getName());
        System.out.println("[DEBUG_LOG] Saved product code: " + savedProduct.getCode());
        System.out.println("[DEBUG_LOG] Saved product category ID: " + savedProduct.getCategoryId());

        // Create and save a product with unique category
        ProductWithUniqueCategoryTable uniqueCategoryTable = new ProductWithUniqueCategoryTable(appContext);
        ProductWithUniqueCategory uniqueCategoryProduct = new ProductWithUniqueCategory();
        uniqueCategoryProduct.setName("Test Product with Unique Category");
        uniqueCategoryProduct.setCategoryId(1);

        // Create and set the category object directly
        Category categoryForUniqueProduct = new Category();
        categoryForUniqueProduct.setId(1);
        categoryForUniqueProduct.setName("Test Category");
        uniqueCategoryProduct.setCategory(categoryForUniqueProduct);

        ProductWithUniqueCategory savedUniqueProduct = uniqueCategoryTable.saveProduct(uniqueCategoryProduct);
        System.out.println("[DEBUG_LOG] Saved unique product ID: " + savedUniqueProduct.getId());
        System.out.println("[DEBUG_LOG] Saved unique product name: " + savedUniqueProduct.getName());
        System.out.println("[DEBUG_LOG] Saved unique product category ID: " + savedUniqueProduct.getCategoryId());
    }

    @Test
    public void testUniqueColumnConstraint() {
        // Create a product with the same unique code as the one in setupTestData
        ProductWithUniqueCodeTable productTable = new ProductWithUniqueCodeTable(appContext);
        ProductWithUniqueCode product = new ProductWithUniqueCode();
        product.setName("Another Product");
        product.setCode("UNIQUE123"); // Same code as the product in setupTestData
        product.setCategoryId(1);

        // Attempt to save the product with the duplicate code
        try {
            productTable.saveProduct(product);
            fail("Expected SQLiteException due to unique constraint violation");
        } catch (SQLiteException e) {
            // Expected exception
            assertTrue("Exception message should mention UNIQUE constraint", 
                    e.getMessage().contains("UNIQUE constraint failed") || 
                    e.getMessage().contains("column code is not unique"));
        }
    }

    @Test
    public void testUniqueJoinConstraint() {
        // Create a product with the same category as the one in setupTestData
        // This should fail because the category_id column has a unique constraint
        ProductWithUniqueCategoryTable uniqueCategoryTable = new ProductWithUniqueCategoryTable(appContext);
        ProductWithUniqueCategory uniqueCategoryProduct = new ProductWithUniqueCategory();
        uniqueCategoryProduct.setName("Another Product with Unique Category");
        uniqueCategoryProduct.setCategoryId(1); // Same category ID as the product in setupTestData

        // Create and set the category object directly
        Category categoryForUniqueProduct = new Category();
        categoryForUniqueProduct.setId(1);
        categoryForUniqueProduct.setName("Test Category");
        uniqueCategoryProduct.setCategory(categoryForUniqueProduct);

        // Attempt to save the product with the duplicate category
        try {
            uniqueCategoryTable.saveProduct(uniqueCategoryProduct);
            fail("Expected SQLiteException due to unique constraint violation");
        } catch (SQLiteException e) {
            // Expected exception
            assertTrue("Exception message should mention UNIQUE constraint", 
                    e.getMessage().contains("UNIQUE constraint failed") || 
                    e.getMessage().contains("column category_id is not unique"));
        }
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

    @Table(name = "products_with_unique_code")
    public static class ProductWithUniqueCode {
        @Column(name = "id", isPrimaryKey = true, isAutoIncrement = true)
        private int id;

        @Column(name = "name")
        private String name;

        // This column has a unique constraint
        @Column(name = "code", isUnique = true)
        private String code;

        @Column(name = "category_id")
        private int categoryId;

        @Join(targetName = "category_id", relationShip = Category.class, source = "id")
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

        // This join has a unique constraint
        @Join(targetName = "category_id", relationShip = Category.class, source = "id", isUnique = true)
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