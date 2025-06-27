package com.jef.sqlite.management.interfaces;


import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Join {

    String targetName();
    Class<?> relationShip();
    String source();
    String defaultValue();
    boolean permitNull() default true;


}
