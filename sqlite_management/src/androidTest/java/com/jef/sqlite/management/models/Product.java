package com.jef.sqlite.management.models;

import com.jef.sqlite.management.interfaces.Column;
import com.jef.sqlite.management.interfaces.Join;
import com.jef.sqlite.management.interfaces.Table;

@Table(name = "products")
public class Product {

    @Column(name = "id", isPrimaryKey = true, isAutoIncrement = true)
    private int id;
    @Column(name = "name")
    private String name;
    @Join(targetName = "line", relationShip = Line.class, source = "id")
    private Line line;

    public Product() {
    }

    public Product(int id, String name, Line line) {
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

    public Line getLine() {
        return line;
    }

    public void setLine(Line line) {
        this.line = line;
    }

}