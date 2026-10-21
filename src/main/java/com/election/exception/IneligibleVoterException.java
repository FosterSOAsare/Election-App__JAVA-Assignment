package com.election.exception;

/** The user is unknown or is not allowed to vote, for example an administrator. */
public class IneligibleVoterException extends ElectionException {

    public IneligibleVoterException(String message) {
        super(message);
    }
}
