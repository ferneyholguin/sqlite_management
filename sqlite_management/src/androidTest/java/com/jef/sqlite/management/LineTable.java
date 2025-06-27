package com.jef.sqlite.management;

import android.content.Context;

import androidx.annotation.NonNull;

import com.jef.sqlite.management.Query.QueryFactory;

import java.util.List;

/**
 * Table class for Line entity
 */
public class LineTable extends SQLiteTable<Line> {

    public LineTable(@NonNull Context context) {
        super(new Management(context));
    }

    private LineQuery query() {
        return QueryFactory.create(LineQuery.class, Line.class, getManagement());
    }

    /**
     * Get all lines
     * @return a list of all lines
     */
    public List<Line> getAllLines() {
        return query().findAll();
    }

    /**
     * Save a line to the database
     * @param line the line to save
     * @return the saved line with any auto-generated values (like auto-increment IDs)
     */
    public Line saveLine(Line line) {
        return query().save(line);
    }

    /**
     * Query interface for Line entity
     */
    public interface LineQuery {
        List<Line> findAll();
        Line save(Line line);
    }
}