package com.jef.sqlite.management;

import com.jef.sqlite.management.interfaces.Table;

@Table(name = "products")
public class Product {

    private int id;
    private String name;
    private String line;

    public Product() {
    }

    public Product(int id, String name, String line) {
        this.id = id;
        this.name = name;
        this.line = line;
    }

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

    public String getLine() {
        return line;
    }

    public void setLine(String line) {
        this.line = line;
    }



}
