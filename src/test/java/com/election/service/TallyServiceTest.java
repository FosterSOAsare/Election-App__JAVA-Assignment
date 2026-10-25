package com.election.service;

import com.election.TestData;
import com.election.exception.ElectionStateException;
import com.election.model.BallotType;
import com.election.model.CandidateResult;
import com.election.model.Category;
import com.election.model.CategoryResult;
import com.election.model.Election;
import com.election.model.ElectionStatus;
import com.election.model.SingleChoiceBallot;
import com.election.model.YesNoBallot;
import com.election.model.YesNoResult;
import com.election.repository.ElectionRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TallyServiceTest {

    /** E1 with CAT1 President (C1 Ana, C2 Ben) and CAT2 a Yes / No question, plus three ballots. */
    private static Election electionWithBallots() throws Exception {
        Election election = TestData.draftWithPresident("E1");
        TestData.category(election, "CAT2", "Raise the fee?", BallotType.YES_NO, 1);
        election.addBallot(new SingleChoiceBallot("V001", "E1", "CAT1", List.of("C1")));
        election.addBallot(new SingleChoiceBallot("V002", "E1", "CAT1", List.of("C1")));
        election.addBallot(new YesNoBallot("V001", "E1", "CAT2", List.of("NO")));
        return election;
    }

    private static TallyService tally(Election... elections) {
        return new TallyService(new ElectionRepository(List.of(elections)), new CountingMethodRegistry());
    }

    @Test
    void aClosedElectionGetsOneResultPerCategoryInCategoryOrder() throws Exception {
        Election election = electionWithBallots();
        election.open();
        election.close();

        Map<Category, CategoryResult> results = tally(election).count(election);

        assertEquals(List.of("CAT1", "CAT2"), results.keySet().stream().map(Category::getId).toList());
    }

    @Test
    void eachCategoryIsCountedByTheMethodForItsTypeWithOnlyItsOwnBallots() throws Exception {
        Election election = electionWithBallots();
        election.open();
        election.close();

        List<CategoryResult> results = List.copyOf(tally(election).count(election).values());

        CandidateResult president = assertInstanceOf(CandidateResult.class, results.get(0));
        assertEquals(2, president.getBallotCount());
        assertEquals("C1", president.getWinners().get(0).getId());

        YesNoResult feeRise = assertInstanceOf(YesNoResult.class, results.get(1));
        assertEquals(1, feeRise.getBallotCount());
        assertEquals(1, feeRise.getNo());
    }

    @Test
    void aCategoryWithoutBallotsStillGetsAResult() throws Exception {
        Election election = TestData.draftWithPresident("E1");
        election.open();
        election.close();

        CategoryResult result = tally(election).count(election).values().iterator().next();

        assertEquals(0, result.getBallotCount());
    }

    @Test
    void resultsOnlyExistOnceAnElectionIsClosed() throws Exception {
        Election draft = electionWithBallots();
        Election open = TestData.election("E2", ElectionStatus.OPEN);
        TallyService tally = tally(draft, open);

        assertThrows(ElectionStateException.class, () -> tally.count(draft));
        ElectionStateException e = assertThrows(ElectionStateException.class, () -> tally.count(open));
        assertTrue(e.getMessage().contains("OPEN"));
    }

    @Test
    void closedElectionsListsOnlyClosedOnes() {
        TallyService tally = tally(TestData.election("E1", ElectionStatus.CLOSED), TestData.draft("E2"),
                TestData.election("E3", ElectionStatus.OPEN), TestData.election("E4", ElectionStatus.CLOSED));

        assertEquals(List.of("E1", "E4"), tally.closedElections().stream().map(Election::getId).toList());
    }
}
