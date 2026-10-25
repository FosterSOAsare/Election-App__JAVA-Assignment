package com.election;

import com.election.exception.DuplicateVoteException;
import com.election.exception.ElectionStateException;
import com.election.model.BallotType;
import com.election.model.CandidateResult;
import com.election.model.CategoryResult;
import com.election.model.Deadlines;
import com.election.model.Election;
import com.election.model.ElectionStatus;
import com.election.model.User;
import com.election.model.Voter;
import com.election.model.YesNoResult;
import com.election.repository.ElectionRepository;
import com.election.service.BallotFactory;
import com.election.service.CountingMethodRegistry;
import com.election.service.ElectionManagementService;
import com.election.service.ElectionManagementService.NewCategory;
import com.election.service.TallyService;
import com.election.service.VotingService;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The whole path an election takes, through the same services the menus use. */
class LifecycleTest {

    private final Clock clock = TestData.clockAt("2026-11-01T12:00:00");
    private final ElectionRepository repository = new ElectionRepository(List.of());
    private final ElectionManagementService management = new ElectionManagementService(repository, clock);
    private final VotingService voting = new VotingService(repository, clock, new BallotFactory());
    private final TallyService tally = new TallyService(repository, new CountingMethodRegistry());

    private void vote(User voter, Election election, String president, String fee) throws Exception {
        voting.submitBallots(voter, election, List.of(
                voting.prepareBallot(voter, election, "CAT1", List.of(president)),
                voting.prepareBallot(voter, election, "CAT2", List.of(fee))));
    }

    @Test
    void createOpenVoteCloseAndCount() throws Exception {
        User admin = TestData.admin();

        // 1. Create a draft with one category, then add a second one.
        Election election = management.createElection(admin, "Chess Club 2026", Deadlines.parse("2026-11-20 18:00"),
                List.of(new NewCategory("President", BallotType.SELECT_ONE, 1, List.of("Ana", "Ben"))));
        management.addCategories(admin, election,
                List.of(new NewCategory("Raise the fee?", BallotType.YES_NO, 1, List.of())));
        assertEquals(ElectionStatus.DRAFT, election.getStatus());
        assertThrows(ElectionStateException.class, () -> tally.count(election));

        // 2. Open it: now it can be voted in, but no longer edited.
        management.openElection(admin, election);
        assertEquals(List.of(election), voting.openElections());
        assertThrows(ElectionStateException.class,
                () -> management.updateElectionDetails(admin, election, "Renamed", null));

        // 3. Three voters vote; one tries to vote twice.
        Voter v1 = new Voter("V001");
        Voter v2 = new Voter("V002");
        Voter v3 = new Voter("V003");
        vote(v1, election, "C1", "YES");
        vote(v2, election, "c1", "no");
        vote(v3, election, "C2", "YES");
        assertThrows(DuplicateVoteException.class, () -> vote(v1, election, "C2", "NO"));
        assertEquals(6, election.getBallots().size());

        // 4. Close it: no more votes, and the results exist.
        management.closeElection(admin, election);
        assertThrows(ElectionStateException.class, () -> vote(new Voter("V004"), election, "C2", "NO"));
        assertTrue(tally.closedElections().contains(election));

        List<CategoryResult> results = List.copyOf(tally.count(election).values());
        CandidateResult president = (CandidateResult) results.get(0);
        assertEquals("Ana", president.getWinners().get(0).getName());
        assertEquals(2, president.votesFor("C1"));
        assertEquals(1, president.votesFor("C2"));

        YesNoResult fee = (YesNoResult) results.get(1);
        assertEquals(2, fee.getYes());
        assertEquals(1, fee.getNo());
        assertTrue(fee.isPassed());
    }
}
