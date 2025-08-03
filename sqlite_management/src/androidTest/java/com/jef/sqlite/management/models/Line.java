package com.jef.sqlite.management.models;

import com.jef.sqlite.management.interfaces.Column;
import com.jef.sqlite.management.interfaces.Table;

import java.util.Date;

@Table(name = "lines")
public class Line {

    @Column(name = "id", primaryKey = true, autoIncrement = true)
    private int id;

    @Column(name = "name")
    private String name;

    @Column(name = "date_creation")
    private Date dateCreation;

    public Line() {
        this.dateCreation = new Date(); // Initialize with current date
    }

    public Line(int id, String name) {
        this.id = id;
        this.name = name;
        this.dateCreation = new Date(); // Initialize with current date
    }

    public Line(int id, String name, Date dateCreation) {
        this.id = id;
        this.name = name;
        this.dateCreation = dateCreation;
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

    public Date getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(Date dateCreation) {
        this.dateCreation = dateCreation;
    }
}
