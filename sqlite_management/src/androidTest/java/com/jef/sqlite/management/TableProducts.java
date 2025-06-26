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




}
