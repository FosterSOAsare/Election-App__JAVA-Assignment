package com.election.model;

import com.election.exception.InvalidBallotException;

import java.util.ArrayList;
import java.util.List;

/** A Select multiple ballot: between 1 and "seats" different candidates of the category. */
public class MultipleChoiceBallot extends Ballot {

    public MultipleChoiceBallot(String voterId, String electionId, String categoryId, List<String> choices) {
        super(voterId, electionId, categoryId, choices);
    }

    @Override
    public void validate(Category category) throws InvalidBallotException {
        List<String> typed = getChoices();
        if (typed.isEmpty() || typed.size() > category.getSeats()) {
            throw new InvalidBallotException("Choose between 1 and " + category.getSeats() + " candidates for "
                    + category.getName());
        }
        List<String> ids = new ArrayList<>();
        for (String choice : typed) {
            String id = candidateId(category, choice);
            if (ids.contains(id)) {
                throw new InvalidBallotException(id + " was chosen twice");
            }
            ids.add(id);
        }
        useChoices(ids);
    }
}
