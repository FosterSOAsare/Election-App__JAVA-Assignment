package com.election.model;

import com.election.exception.InvalidBallotException;

import java.util.List;
import java.util.Locale;

/** A Yes / No ballot: exactly YES or NO, typed in any case. */
public class YesNoBallot extends Ballot {

    public YesNoBallot(String voterId, String electionId, String categoryId, List<String> choices) {
        super(voterId, electionId, categoryId, choices);
    }

    @Override
    public void validate(Category category) throws InvalidBallotException {
        // Locale.ROOT makes "yes" upper-case to "YES" the same way on every computer.
        String answer = getChoices().size() == 1 ? getChoices().get(0).toUpperCase(Locale.ROOT) : "";
        if (!answer.equals(YesNoResult.YES) && !answer.equals(YesNoResult.NO)) {
            throw new InvalidBallotException("Answer YES or NO");
        }
        useChoices(List.of(answer));
    }
}
