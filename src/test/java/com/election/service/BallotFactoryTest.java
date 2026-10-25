package com.election.service;

import com.election.TestData;
import com.election.exception.InvalidBallotException;
import com.election.model.Ballot;
import com.election.model.BallotType;
import com.election.model.Category;
import com.election.model.Election;
import com.election.model.MultipleChoiceBallot;
import com.election.model.SingleChoiceBallot;
import com.election.model.YesNoBallot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BallotFactoryTest {

    private final BallotFactory factory = new BallotFactory();
    private Category president;
    private Category committee;
    private Category feeRise;

    @BeforeEach
    void setUp() throws Exception {
        Election election = TestData.draftWithPresident("E1");
        president = election.findCategory("CAT1").orElseThrow();
        committee = TestData.category(election, "CAT2", "Committee", BallotType.SELECT_MULTIPLE, 2);
        TestData.addCandidate(committee, "C3", "Cara");
        TestData.addCandidate(committee, "C4", "Dan");
        feeRise = TestData.category(election, "CAT3", "Raise the fee?", BallotType.YES_NO, 1);
    }

    @Test
    void theCategorysTypeDecidesTheBallotSubclass() throws InvalidBallotException {
        assertInstanceOf(SingleChoiceBallot.class, factory.create("V001", president, List.of("C1")));
        assertInstanceOf(MultipleChoiceBallot.class, factory.create("V001", committee, List.of("C3", "C4")));
        assertInstanceOf(YesNoBallot.class, factory.create("V001", feeRise, List.of("YES")));
    }

    @Test
    void theNewBallotBelongsToTheCategoryAndItsElection() throws InvalidBallotException {
        Ballot ballot = factory.create("V001", committee, List.of("c4"));

        assertEquals("V001", ballot.getVoterId());
        assertEquals("E1", ballot.getElectionId());
        assertEquals("CAT2", ballot.getCategoryId());
        assertEquals(List.of("C4"), ballot.getChoices());
    }

    @Test
    void theBallotIsCheckedBeforeItIsReturned() {
        assertThrows(InvalidBallotException.class, () -> factory.create("V001", president, List.of("C3")));
        assertThrows(InvalidBallotException.class, () -> factory.create("V001", committee, List.of("C3", "C4", "C1")));
        assertThrows(InvalidBallotException.class, () -> factory.create("V001", feeRise, List.of("MAYBE")));
    }
}
