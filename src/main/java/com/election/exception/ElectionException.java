package com.election.exception;

/** A business rule was broken. Checked, so every caller has to decide what to do with it. */
public class ElectionException extends Exception {

    public ElectionException(String message) {
        super(message);
    }
}
