package com.jef.sqlite.management.interfaces;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Column {


    String name() default "";

    boolean permitNull() default true;

    boolean isPrimaryKey() default false;

    boolean isAutoIncrement() default false;

    String defaultValue() default "";


}
