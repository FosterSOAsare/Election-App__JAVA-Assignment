package com.election.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YesNoResultTest {

    private final Category feeRise = new Category("CAT3", "E1", "Raise the fee?", BallotType.YES_NO, 1);

    @Test
    void moreYesThanNoPasses() {
        YesNoResult result = new YesNoResult(feeRise, 10, 7, 3);

        assertTrue(result.isPassed());
        assertEquals(7, result.getYes());
        assertEquals(3, result.getNo());
        assertEquals(10, result.getBallotCount());
        assertEquals(List.of("YES 7   NO 3", "Passed (70.0% yes)"), result.summary().lines().toList());
    }

    @Test
    void moreNoThanYesIsRejected() {
        YesNoResult result = new YesNoResult(feeRise, 4, 1, 3);

        assertFalse(result.isPassed());
        assertEquals("Rejected (25.0% yes)", result.summary().lines().toList().get(1));
    }

    @Test
    void aTieIsRejectedBecauseAChangeNeedsMoreSupportThanOpposition() {
        YesNoResult result = new YesNoResult(feeRise, 2, 1, 1);

        assertFalse(result.isPassed());
        assertEquals("Rejected (50.0% yes)", result.summary().lines().toList().get(1));
    }

    @Test
    void noVotesIsRejected() {
        YesNoResult result = new YesNoResult(feeRise, 0, 0, 0);

        assertFalse(result.isPassed());
        assertEquals(List.of("YES 0   NO 0", "Rejected (no votes)"), result.summary().lines().toList());
    }

    @Test
    void thePercentageUsesAFullStopOnEveryComputer() {
        Locale before = Locale.getDefault();
        try {
            // German writes 66,7; the result must still read 66.7.
            Locale.setDefault(Locale.GERMANY);
            YesNoResult result = new YesNoResult(feeRise, 9, 6, 3);
            assertEquals("Passed (66.7% yes)", result.summary().lines().toList().get(1));
        } finally {
            Locale.setDefault(before);
        }
    }

    @Test
    void votesCanBeReadByOption() {
        YesNoResult result = new YesNoResult(feeRise, 10, 7, 3);

        assertEquals(7, result.votesFor(YesNoResult.YES));
        assertEquals(3, result.votesFor(YesNoResult.NO));
        assertEquals(0, result.votesFor("MAYBE"));
    }
}
