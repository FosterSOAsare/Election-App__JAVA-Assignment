package com.election.exception;

/**
 * A data file could not be read, or one of its rows is wrong. The message always says which file and line,
 * and the original problem is kept as the cause.
 */
public class DataLoadException extends Exception {

    public DataLoadException(String message) {
        super(message);
    }

    public DataLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
