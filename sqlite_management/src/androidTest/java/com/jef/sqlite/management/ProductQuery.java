package com.jef.sqlite.management;

import com.jef.sqlite.management.interfaces.DynamicQuery;

import java.util.List;

public interface ProductQuery extends DynamicQuery<Product> {

    /**
     * Find products by name
     * @param name the name to search for
     * @return a list of products with the given name
     */
    List<Product> findByName(String name);

    /**
     * Find all products ordered by name
     * @param direction the sort direction ("ASC" or "DESC")
     * @return a list of all products ordered by name
     */
    List<Product> findAllOrderByName(String direction);

    /**
     * Find all products ordered by id
     * @param direction the sort direction ("ASC" or "DESC")
     * @return a list of all products ordered by id
     */
    List<Product> findAllOrderById(String direction);

    /**
     * Find all products with a specific line, ordered by name
     * @param line the product line to search for
     * @param direction the sort direction ("ASC" or "DESC")
     * @return a list of products with the given line, ordered by name
     */
    List<Product> findAllByLineOrderByName(String line, String direction);

    /**
     * Find all products with a specific name, ordered by id
     * @param name the product name to search for
     * @param direction the sort direction ("ASC" or "DESC")
     * @return a list of products with the given name, ordered by id
     */
    List<Product> findAllByNameOrderById(String name, String direction);

}
