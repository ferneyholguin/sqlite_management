package com.jef.sqlite.management.queries;

import android.content.ContentValues;

import com.jef.sqlite.management.interfaces.DynamicQuery;
import com.jef.sqlite.management.models.Line;

import java.util.List;
import java.util.Optional;

/**
 * Query interface for Line entity
 */
public interface LineQuery extends DynamicQuery<Line> {
    List<Line> findAll();
    Line save(Line line);
    boolean validate(Line line);
    Optional<Line> findById(int id);
    int updateById(ContentValues values, int id);
    int updateNameWhereId(String name, int id);
}
