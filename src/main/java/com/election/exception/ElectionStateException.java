package com.election.exception;

/** The action is not allowed in the election's current status, for example voting in a DRAFT or CLOSED one. */
public class ElectionStateException extends ElectionException {

    public ElectionStateException(String message) {
        super(message);
    }
}
