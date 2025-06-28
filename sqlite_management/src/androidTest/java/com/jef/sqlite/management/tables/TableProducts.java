package com.jef.sqlite.management.tables;

import android.content.Context;

import androidx.annotation.NonNull;

import com.jef.sqlite.management.Management;
import com.jef.sqlite.management.SQLiteTable;
import com.jef.sqlite.management.Query.QueryFactory;
import com.jef.sqlite.management.models.Product;
import com.jef.sqlite.management.queries.ProductQuery;

import java.util.List;

public class TableProducts extends SQLiteTable<Product>{

    public TableProducts(@NonNull Context context) {
        super(new Management(context));
    }

    private ProductQuery query() {
        return QueryFactory.create(ProductQuery.class, Product.class, getManagement());
    }

    public List<Product> getProductsByName(String name) {
        return query().findByName(name);
    }

    /**
     * Get all products
     * @return a list of all products
     */
    public List<Product> getAllProducts() {
        return query().findAll();
    }

    /**
     * Get all products ordered by name
     * @param ascending true for ascending order, false for descending
     * @return a list of all products ordered by name
     */
    public List<Product> getAllProductsOrderedByName(boolean ascending) {
        return ascending ? query().findAllOrderByNameAsc() : query().findAllOrderByNameDesc();
    }

    /**
     * Get all products ordered by id
     * @param ascending true for ascending order, false for descending
     * @return a list of all products ordered by id
     */
    public List<Product> getAllProductsOrderedById(boolean ascending) {
        return ascending ? query().findAllOrderByIdAsc() : query().findAllOrderByIdDesc();
    }

    /**
     * Get all products with a specific name, ordered by id
     * @param name the product name to search for
     * @param ascending true for ascending order, false for descending
     * @return a list of products with the given name, ordered by id
     */
    public List<Product> getProductsByNameOrderedById(String name, boolean ascending) {
        return ascending ? query().findAllByNameOrderByIdAsc(name) : query().findAllByNameOrderByIdDesc(name);
    }

    /**
     * Save a product to the database
     * @param product the product to save
     * @return the saved product with any auto-generated values (like auto-increment IDs)
     */
    public Product saveProduct(Product product) {
        return query().save(product);
    }

}
