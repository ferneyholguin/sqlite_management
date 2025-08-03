package com.jef.sqlite.management.tables;

import android.content.Context;

import androidx.annotation.NonNull;

import com.jef.sqlite.management.Management;
import com.jef.sqlite.management.SQLiteTable;
import com.jef.sqlite.management.Query.QueryFactory;
import com.jef.sqlite.management.exceptions.SQLiteException;
import com.jef.sqlite.management.models.Product;
import com.jef.sqlite.management.queries.ProductQuery;

import java.util.List;

public class ProductsTable extends SQLiteTable<Product>{

    public ProductsTable(@NonNull Context context) {
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
     * @return the saved product with the ID set from the database
     */
    public Product saveProduct(Product product) {
        long id = query().save(product);
        product.setId((int) id);
        return product;
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
     * Update product name by id
     * @param name the new name
     * @param id the id of the product to update
     * @return the number of rows updated
     */
    public int updateProductNameById(String name, int id) {
        return query().updateNameById(name, id);
    }

    /**
     * Update product active status by id
     * @param active the new active status
     * @param id the id of the product to update
     * @return the number of rows updated
     */
    public int updateProductActiveById(boolean active, int id) {
        return query().updateActiveById(active, id);
    }

    /**
     * Update product name and active status by id
     * @param name the new name
     * @param active the new active status
     * @param id the id of the product to update
     * @return the number of rows updated
     */
    public int updateProductNameAndActiveById(String name, boolean active, int id) {
        return query().updateNameActiveById(name, active, id);
    }

    /**
     * Update product name by line id
     * @param name the new name
     * @param lineId the line id to match
     * @return the number of rows updated
     */
    public int updateProductNameByLineId(String name, int lineId) {
        return query().updateNameByLine(name, lineId);
    }

    /**
     * Validate a product against database constraints
     * @param product the product to validate
     * @return true if the product is valid
     * @throws SQLiteException if the product is invalid
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
     * Update product name and active status by name and line id
     * @param newName the new name
     * @param newActive the new active status
     * @param nameToMatch the name to match
     * @param lineIdToMatch the line id to match
     * @return the number of rows updated
     */
    public int updateProductNameActiveByNameAndLineId(String newName, boolean newActive, String nameToMatch, int lineIdToMatch) {
        return query().updateNameActiveByNameAndLine(newName, newActive, nameToMatch, lineIdToMatch);
    }

    /**
     * Check if a product exists by id
     * @param id the id to check
     * @return true if a product with the given id exists, false otherwise
     */
    public boolean productExistsById(int id) {
        return query().existsById(id);
    }

    /**
     * Check if a product exists by name
     * @param name the name to check
     * @return true if a product with the given name exists, false otherwise
     */
    public boolean productExistsByName(String name) {
        return query().existsByName(name);
    }

    public int deleteById(int id) {
        return query().deleteById(id);
    }




}
