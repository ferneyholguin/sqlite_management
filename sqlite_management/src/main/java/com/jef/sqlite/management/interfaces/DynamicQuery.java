package com.jef.sqlite.management.interfaces;

import java.util.List;

public interface DynamicQuery<T> {

    List<T> findBy(String fieldName, Object value);

}
