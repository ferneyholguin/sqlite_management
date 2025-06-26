package com.jef.sqlite.management;

import android.content.Context;

import androidx.annotation.Nullable;

public class Management extends SQLiteManagement {

    private static final String name = "management";
    private static final int version = 1;

    public Management(@Nullable Context context) {
        super(context, name, version);
    }

}
