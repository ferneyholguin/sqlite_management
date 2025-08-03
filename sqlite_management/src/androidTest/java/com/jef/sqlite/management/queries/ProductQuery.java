package com.jef.sqlite.management.queries;

import com.jef.sqlite.management.interfaces.DynamicQuery;
import com.jef.sqlite.management.models.Product;

import java.util.List;
import java.util.Optional;

public interface ProductQuery extends DynamicQuery<Product> {

    /**
     * Check if a product exists by id
     * @param id the id to check
     * @return true if a product with the given id exists, false otherwise
     */
    boolean existsById(int id);

    /**
     * Check if a product exists by name
     * @param name the name to check
     * @return true if a product with the given name exists, false otherwise
     */
    boolean existsByName(String name);

    /**
     * Find products by name and active status
     * @param name the name to search for
     * @param active the active status to search for
     * @return a list of products with the given name and active status
     */
    List<Product> findByNameAndActive(String name, boolean active);

    /**
     * Find products by name or active status
     * @param name the name to search for
     * @param active the active status to search for
     * @return a list of products with the given name or active status
     */
    List<Product> findByNameOrActive(String name, boolean active);

    /**
     * Find products by name
     * @param name the name to search for
     * @return a list of products with the given name
     */
    List<Product> findByName(String name);

    /**
     * Find all products ordered by name in ascending order
     * @return a list of all products ordered by name in ascending order
     */
    List<Product> findAllOrderByNameAsc();

    /**
     * Find all products ordered by name in descending order
     * @return a list of all products ordered by name in descending order
     */
    List<Product> findAllOrderByNameDesc();

    /**
     * Find all products ordered by id in ascending order
     * @return a list of all products ordered by id in ascending order
     */
    List<Product> findAllOrderByIdAsc();

    /**
     * Find all products ordered by id in descending order
     * @return a list of all products ordered by id in descending order
     */
    List<Product> findAllOrderByIdDesc();

    /**
     * Find all products with a specific name, ordered by id in ascending order
     * @param name the product name to search for
     * @return a list of products with the given name, ordered by id in ascending order
     */
    List<Product> findAllByNameOrderByIdAsc(String name);

    /**
     * Find all products with a specific name, ordered by id in descending order
     * @param name the product name to search for
     * @return a list of products with the given name, ordered by id in descending order
     */
    List<Product> findAllByNameOrderByIdDesc(String name);

    Optional<Product> findById(int id);

    /**
     * Update a product by id
     * @param values the values to update
     * @param id the id of the product to update
     * @return the number of rows updated
     */
    int updateById(android.content.ContentValues values, int id);

    /**
     * Update products by name
     * @param values the values to update
     * @param name the name of the products to update
     * @return the number of rows updated
     */
    int updateByName(android.content.ContentValues values, String name);

    /**
     * Update product name by id
     * @param name the new name
     * @param id the id of the product to update
     * @return the number of rows updated
     */
    int updateNameById(String name, int id);

    /**
     * Update product active status by id
     * @param active the new active status
     * @param id the id of the product to update
     * @return the number of rows updated
     */
    int updateActiveById(boolean active, int id);

    /**
     * Update product name and active status by id
     * @param name the new name
     * @param active the new active status
     * @param id the id of the product to update
     * @return the number of rows updated
     */
    int updateNameActiveById(String name, boolean active, int id);

    /**
     * Update product name by line id
     * @param name the new name
     * @param lineId the line id to match
     * @return the number of rows updated
     */
    int updateNameByLine(String name, int lineId);

    /**
     * Update all products' name
     * @param name the new name
     * @return the number of rows updated
     */
    int updateName(String name);

    /**
     * Update product name and active status by name and line id
     * @param newName the new name
     * @param newActive the new active status
     * @param nameToMatch the name to match
     * @param lineIdToMatch the line id to match
     * @return the number of rows updated
     */
    int updateNameActiveByNameAndLine(String newName, boolean newActive, String nameToMatch, int lineIdToMatch);

    int deleteById(int id);


}
