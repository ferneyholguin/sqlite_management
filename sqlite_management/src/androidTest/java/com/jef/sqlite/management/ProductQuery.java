package com.jef.sqlite.management;

import com.jef.sqlite.management.interfaces.DynamicQuery;

import java.util.List;

public interface ProductQuery extends DynamicQuery<Product> {

    List<Product> findByName(String name);

}
