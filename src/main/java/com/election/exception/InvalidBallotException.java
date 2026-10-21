package com.election.exception;

/**
 * The choices do not fit the category: unknown category, a candidate from another category,
 * too many or too few choices, the same candidate twice, or an answer other than YES or NO.
 */
public class InvalidBallotException extends ElectionException {

    public InvalidBallotException(String message) {
        super(message);
    }
}
