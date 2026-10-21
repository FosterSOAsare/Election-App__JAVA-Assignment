package com.election.exception;

/** The user's role, or their relation to the election, does not allow this action. */
public class UnauthorizedActionException extends ElectionException {

    public UnauthorizedActionException(String message) {
        super(message);
    }
}
