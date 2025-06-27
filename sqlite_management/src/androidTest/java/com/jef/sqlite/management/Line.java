package com.jef.sqlite.management;

import com.jef.sqlite.management.interfaces.Column;
import com.jef.sqlite.management.interfaces.Table;

@Table(name = "lines")
public class Line {

    @Column(name = "id", isPrimaryKey = true, isAutoIncrement = true)
    private int id;

    @Column(name = "name")
    private String name;

    public Line() {
    }

    public Line(int id, String name) {
        this.id = id;
        this.name = name;
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



}
