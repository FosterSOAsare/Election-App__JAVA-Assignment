package com.election.service;

import com.election.exception.InvalidBallotException;
import com.election.model.Ballot;
import com.election.model.Category;
import com.election.model.MultipleChoiceBallot;
import com.election.model.SingleChoiceBallot;
import com.election.model.YesNoBallot;

import java.util.List;

/**
 * Builds the right Ballot subclass for a category and has it validate itself.
 * The ballot type belongs to the category, so a ballot can never claim a type of its own.
 * Votes cast in the console and ballots loaded from ballots.csv both come through here.
 */
public class BallotFactory {

    public Ballot create(String voterId, Category category, List<String> choices) throws InvalidBallotException {
        Ballot ballot = switch (category.getBallotType()) {
            case SELECT_ONE -> new SingleChoiceBallot(voterId, category.getElectionId(), category.getId(), choices);
            case SELECT_MULTIPLE -> new MultipleChoiceBallot(voterId, category.getElectionId(), category.getId(),
                    choices);
            case YES_NO -> new YesNoBallot(voterId, category.getElectionId(), category.getId(), choices);
        };
        ballot.validate(category);
        return ballot;
    }
}
