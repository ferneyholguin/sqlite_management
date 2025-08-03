package com.jef.sqlite.management.queries;

import android.content.ContentValues;

import com.jef.sqlite.management.interfaces.DynamicQuery;
import com.jef.sqlite.management.models.Line;

import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Query interface for Line entity
 */
public interface LineQuery extends DynamicQuery<Line> {


    List<Line> findAll();
    long save(Line line);
    boolean validate(Line line);
    Optional<Line> findById(int id);
    int updateById(ContentValues values, int id);
    int updateNameById(String name, int id);
    int updateDateCreationById(Date dateCreation, int id);


}
