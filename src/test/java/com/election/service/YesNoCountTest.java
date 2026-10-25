package com.election.service;

import com.election.model.Ballot;
import com.election.model.BallotType;
import com.election.model.Category;
import com.election.model.YesNoBallot;
import com.election.model.YesNoResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YesNoCountTest {

    private final YesNoCount counting = new YesNoCount();
    private final Category feeRise = new Category("CAT2", "E4", "Extend opening hours?", BallotType.YES_NO, 1);

    private static List<Ballot> answers(int yes, int no) {
        List<Ballot> ballots = new ArrayList<>();
        for (int i = 0; i < yes; i++) {
            ballots.add(new YesNoBallot("V" + i, "E4", "CAT2", List.of("YES")));
        }
        for (int i = 0; i < no; i++) {
            ballots.add(new YesNoBallot("W" + i, "E4", "CAT2", List.of("NO")));
        }
        return ballots;
    }

    @Test
    void moreYesThanNoPasses() {
        // Seed E4: YES 7, NO 3.
        YesNoResult result = (YesNoResult) counting.count(feeRise, answers(7, 3));

        assertTrue(result.isPassed());
        assertEquals(7, result.getYes());
        assertEquals(3, result.getNo());
        assertEquals(10, result.getBallotCount());
    }

    @Test
    void winningByOneVoteIsEnough() {
        assertTrue(((YesNoResult) counting.count(feeRise, answers(5, 4))).isPassed());
    }

    @Test
    void aTieIsRejected() {
        assertFalse(((YesNoResult) counting.count(feeRise, answers(3, 3))).isPassed());
    }

    @Test
    void moreNoThanYesIsRejected() {
        assertFalse(((YesNoResult) counting.count(feeRise, answers(1, 2))).isPassed());
    }

    @Test
    void noBallotsIsRejected() {
        YesNoResult result = (YesNoResult) counting.count(feeRise, List.of());

        assertFalse(result.isPassed());
        assertEquals(0, result.getYes());
        assertEquals(0, result.getNo());
    }

    @Test
    void answersInAnyCaseAreCountedButAnythingElseIsLeftOut() {
        List<Ballot> ballots = List.of(
                new YesNoBallot("V1", "E4", "CAT2", List.of("yes")),
                new YesNoBallot("V2", "E4", "CAT2", List.of("No")),
                new YesNoBallot("V3", "E4", "CAT2", List.of("MAYBE")),
                new YesNoBallot("V4", "E4", "CAT2", List.of("YES", "NO")));

        YesNoResult result = (YesNoResult) counting.count(feeRise, ballots);

        assertEquals(1, result.getYes());
        assertEquals(1, result.getNo());
        assertEquals(4, result.getBallotCount());
    }
}
