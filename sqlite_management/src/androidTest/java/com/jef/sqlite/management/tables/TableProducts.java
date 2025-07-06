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
        return QueryFactory.create(ProductQuery.class, getManagement());
    }

    public List<Product> getProductsByName(String name) {
        return query().findByName(name);
    }

    /**
     * Get a product by id
     * @param id the id of the product to find
     * @return an Optional containing the product if found, or empty if not found
     */
    public java.util.Optional<Product> getProductById(int id) {
        return query().findById(id);
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

    /**
     * Update a product by id
     * @param values the values to update
     * @param id the id of the product to update
     * @return the number of rows updated
     */
    public int updateProductById(android.content.ContentValues values, int id) {
        return query().updateById(values, id);
    }

    /**
     * Update products by name
     * @param values the values to update
     * @param name the name of the products to update
     * @return the number of rows updated
     */
    public int updateProductByName(android.content.ContentValues values, String name) {
        return query().updateByName(values, name);
    }

    /**
     * Update product name by id
     * @param name the new name
     * @param id the id of the product to update
     * @return the number of rows updated
     */
    public int updateProductNameById(String name, int id) {
        return query().updateNameWhereId(name, id);
    }

    /**
     * Update product active status by id
     * @param active the new active status
     * @param id the id of the product to update
     * @return the number of rows updated
     */
    public int updateProductActiveById(boolean active, int id) {
        return query().updateActiveWhereId(active, id);
    }

    /**
     * Update product name and active status by id
     * @param name the new name
     * @param active the new active status
     * @param id the id of the product to update
     * @return the number of rows updated
     */
    public int updateProductNameAndActiveById(String name, boolean active, int id) {
        return query().updateNameActiveWhereId(name, active, id);
    }

    /**
     * Update product name where line id matches
     * @param name the new name
     * @param lineId the line id to match
     * @return the number of rows updated
     */
    public int updateProductNameByLineId(String name, int lineId) {
        return query().updateNameWhereLineId(name, lineId);
    }

    /**
     * Validate a product against database constraints
     * @param product the product to validate
     * @return true if the product is valid, false otherwise
     */
    public boolean validateProduct(Product product) {
        return query().validate(product);
    }

    /**
     * Find products by name and active status
     * @param name the name to search for
     * @param active the active status to search for
     * @return a list of products with the given name and active status
     */
    public List<Product> getProductsByNameAndActive(String name, boolean active) {
        return query().findByNameAndActive(name, active);
    }

    /**
     * Find products by name or active status
     * @param name the name to search for
     * @param active the active status to search for
     * @return a list of products with the given name or active status
     */
    public List<Product> getProductsByNameOrActive(String name, boolean active) {
        return query().findByNameOrActive(name, active);
    }

    /**
     * Update all products' name
     * @param name the new name
     * @return the number of rows updated
     */
    public int updateAllProductsName(String name) {
        return query().updateName(name);
    }

    /**
     * Update product name and active status where name and line id match
     * @param newName the new name
     * @param newActive the new active status
     * @param nameToMatch the name to match
     * @param lineIdToMatch the line id to match
     * @return the number of rows updated
     */
    public int updateProductNameActiveByNameAndLineId(String newName, boolean newActive, String nameToMatch, int lineIdToMatch) {
        return query().updateNameActiveWhereNameAndLineId(newName, newActive, nameToMatch, lineIdToMatch);
    }
}
