package com.election.model;

import com.election.exception.InvalidBallotException;

import java.util.List;

/** A Select one ballot: exactly one candidate of the category. */
public class SingleChoiceBallot extends Ballot {

    public SingleChoiceBallot(String voterId, String electionId, String categoryId, List<String> choices) {
        super(voterId, electionId, categoryId, choices);
    }

    @Override
    public void validate(Category category) throws InvalidBallotException {
        if (getChoices().size() != 1) {
            throw new InvalidBallotException("Choose exactly 1 candidate for " + category.getName());
        }
        useChoices(List.of(candidateId(category, getChoices().get(0))));
    }
}
