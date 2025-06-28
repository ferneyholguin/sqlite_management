package com.jef.sqlite.management.queries;

import com.jef.sqlite.management.interfaces.DynamicQuery;
import com.jef.sqlite.management.models.Product;

import java.util.List;
import java.util.Optional;

public interface ProductQuery extends DynamicQuery<Product> {

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

}
