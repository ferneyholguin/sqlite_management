package com.jef.sqlite.management.exceptions;

/**
 * Excepción específica para errores relacionados con operaciones SQLite.
 * Esta clase extiende RuntimeException para proporcionar excepciones no verificadas
 * relacionadas con la gestión de bases de datos SQLite.
 */
public class SQLiteException extends RuntimeException {

    /**
     * Constructor por defecto sin mensaje ni causa.
     */
    public SQLiteException() {
    }

    /**
     * Constructor con mensaje de error.
     * 
     * @param message El mensaje detallado del error
     */
    public SQLiteException(String message) {
        super(message);
    }

    /**
     * Constructor con mensaje de error y causa.
     * 
     * @param message El mensaje detallado del error
     * @param cause La causa original del error
     */
    public SQLiteException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructor con causa pero sin mensaje.
     * 
     * @param cause La causa original del error
     */
    public SQLiteException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructor completo con opciones adicionales.
     * 
     * @param message El mensaje detallado del error
     * @param cause La causa original del error
     * @param enableSuppression Si se permite la supresión de excepciones
     * @param writableStackTrace Si se debe escribir el seguimiento de la pila
     */
    public SQLiteException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }



}
