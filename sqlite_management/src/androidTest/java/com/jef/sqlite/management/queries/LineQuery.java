package com.jef.sqlite.management.queries;

import com.jef.sqlite.management.interfaces.DynamicQuery;
import com.jef.sqlite.management.models.Line;

import java.util.List;

/**
 * Query interface for Line entity
 */
public interface LineQuery extends DynamicQuery<Line> {
    List<Line> findAll();
    Line save(Line line);
}
