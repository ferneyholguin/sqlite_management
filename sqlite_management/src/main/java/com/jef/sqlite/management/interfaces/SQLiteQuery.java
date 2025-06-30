package com.jef.sqlite.management.interfaces;


import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface SQLiteQuery {

    String sql();

}
