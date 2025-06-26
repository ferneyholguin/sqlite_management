package com.jef.sqlite.management;

import android.content.Context;

import androidx.annotation.NonNull;

import com.jef.sqlite.management.Query.QueryFactory;

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
        return query().findAllOrderByName(ascending ? "ASC" : "DESC");
    }

    /**
     * Get all products ordered by id
     * @param ascending true for ascending order, false for descending
     * @return a list of all products ordered by id
     */
    public List<Product> getAllProductsOrderedById(boolean ascending) {
        return query().findAllOrderById(ascending ? "ASC" : "DESC");
    }

    /**
     * Get all products with a specific line, ordered by name
     * @param line the product line to search for
     * @param ascending true for ascending order, false for descending
     * @return a list of products with the given line, ordered by name
     */
    public List<Product> getProductsByLineOrderedByName(String line, boolean ascending) {
        return query().findAllByLineOrderByName(line, ascending ? "ASC" : "DESC");
    }

    /**
     * Get all products with a specific name, ordered by id
     * @param name the product name to search for
     * @param ascending true for ascending order, false for descending
     * @return a list of products with the given name, ordered by id
     */
    public List<Product> getProductsByNameOrderedById(String name, boolean ascending) {
        return query().findAllByNameOrderById(name, ascending ? "ASC" : "DESC");
    }

}
