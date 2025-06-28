package com.jef.sqlite.management;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

/**
 * Clase abstracta que extiende SQLiteOpenHelper para gestionar la base de datos SQLite.
 * Proporciona métodos para crear y actualizar la base de datos.
 */
public abstract class SQLiteManagement extends SQLiteOpenHelper {

    /**
     * Constructor para SQLiteManagement.
     * 
     * @param context El contexto de la aplicación
     * @param name El nombre de la base de datos
     * @param version La versión de la base de datos
     */
    public SQLiteManagement(@Nullable Context context, @Nullable String name, int version) {
        super(context, name, null, version);
    }

    /**
     * Se llama cuando la base de datos se crea por primera vez.
     * 
     * @param db La base de datos SQLite
     */
    @Override
    public void onCreate(SQLiteDatabase db) {

    }

    /**
     * Se llama cuando la base de datos necesita ser actualizada.
     * 
     * @param db La base de datos SQLite
     * @param oldVersion La versión antigua de la base de datos
     * @param newVersion La nueva versión de la base de datos
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

























}
