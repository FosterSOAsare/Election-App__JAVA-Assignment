package com.election.exception;

/** The voter has already voted in this category. */
public class DuplicateVoteException extends ElectionException {

    public DuplicateVoteException(String message) {
        super(message);
    }
}
