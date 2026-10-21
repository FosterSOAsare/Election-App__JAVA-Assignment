package com.election.exception;

/** A name is empty or contains characters that NameRules does not allow. */
public class InvalidNameException extends ElectionException {

    public InvalidNameException(String message) {
        super(message);
    }
}
