package com.election;

import com.election.loader.BallotCsvLoader;
import com.election.loader.CandidateCsvLoader;
import com.election.loader.CategoryCsvLoader;
import com.election.loader.ElectionCsvLoader;
import com.election.loader.UserCsvLoader;
import com.election.model.Ballot;
import com.election.model.CandidateResult;
import com.election.model.Candidate;
import com.election.model.Category;
import com.election.model.CategoryResult;
import com.election.model.Election;
import com.election.model.ElectionStatus;
import com.election.model.User;
import com.election.model.YesNoResult;
import com.election.repository.ElectionRepository;
import com.election.repository.UserRepository;
import com.election.service.BallotFactory;
import com.election.service.CountingMethodRegistry;
import com.election.service.TallyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Loads the real files in src/main/resources (Maven runs the tests from the project folder),
 * so a mistake in the seed data or a change that breaks loading is caught here.
 * The expected numbers are the "Expected results" in PLAN.md.
 */
class SeedDataTest {

    private static final Path DATA = Path.of("src/main/resources");

    private UserRepository users;
    private ElectionRepository elections;
    private TallyService tally;

    @BeforeEach
    void loadTheSeedFiles() throws Exception {
        users = new UserRepository(new UserCsvLoader().load(DATA.resolve("users.csv")));
        elections = new ElectionRepository(new ElectionCsvLoader().load(DATA.resolve("elections.csv")));
        new CategoryCsvLoader(elections).load(DATA.resolve("categories.csv"));
        new CandidateCsvLoader(elections).load(DATA.resolve("candidates.csv"));
        new BallotCsvLoader(elections, new BallotFactory()).load(DATA.resolve("ballots.csv"));
        tally = new TallyService(elections, new CountingMethodRegistry());
    }

    private Election election(String id) {
        return elections.findById(id).orElseThrow();
    }

    private List<CategoryResult> results(String electionId) throws Exception {
        return List.copyOf(tally.count(election(electionId)).values());
    }

    private static List<String> winners(CategoryResult result) {
        return ((CandidateResult) result).getWinners().stream().map(Candidate::getId).toList();
    }

    @Test
    void everyFileLoads() {
        assertEquals(6, elections.findAll().size());
        int ballots = elections.findAll().stream().mapToInt(e -> e.getBallots().size()).sum();
        assertEquals(69, ballots);
        assertTrue(users.findById("A1").isPresent());
        assertTrue(users.findById("V010").isPresent());
    }

    @Test
    void draftsHaveNoBallots() {
        for (Election draft : elections.findByStatus(ElectionStatus.DRAFT)) {
            assertTrue(draft.getBallots().isEmpty(), draft.getId());
        }
    }

    @Test
    void everySeededBallotWasCastByAKnownVoterAtMostOncePerCategory() {
        Set<String> seen = new HashSet<>();
        for (Election election : elections.findAll()) {
            for (Ballot ballot : election.getBallots()) {
                User voter = users.findById(ballot.getVoterId()).orElseThrow(() ->
                        new AssertionError("unknown voter " + ballot.getVoterId()));
                assertTrue(voter.canVote(), voter.getId() + " is not allowed to vote");
                assertTrue(seen.add(ballot.getVoterId() + "|" + election.getId() + "|" + ballot.getCategoryId()),
                        ballot.getVoterId() + " voted twice in " + election.getId() + " " + ballot.getCategoryId());
            }
        }
    }

    @Test
    void e3ResultsMatchThePlan() throws Exception {
        List<CategoryResult> e3 = results("E3");

        assertEquals(List.of("C1"), winners(e3.get(0)));                        // President: Daniel Osei 5-3-2
        CandidateResult treasurer = (CandidateResult) e3.get(1);
        assertTrue(treasurer.getWinners().isEmpty());                            // Treasurer: 4-4 tie
        assertEquals(List.of("C4", "C5"), treasurer.getTiedForLastSeat().stream().map(Candidate::getId).toList());
    }

    @Test
    void e4ResultsMatchThePlan() throws Exception {
        List<CategoryResult> e4 = results("E4");

        assertEquals(List.of("C1", "C2", "C3"), winners(e4.get(0)));            // Library committee, 3 seats
        YesNoResult hours = (YesNoResult) e4.get(1);
        assertEquals(7, hours.getYes());
        assertEquals(3, hours.getNo());
        assertTrue(hours.isPassed());
    }

    @Test
    void afterClosingE1ItsResultsMatchThePlan() throws Exception {
        Election e1 = election("E1");
        e1.close();

        Map<Category, CategoryResult> byCategory = tally.count(e1);
        List<CategoryResult> results = List.copyOf(byCategory.values());

        CandidateResult president = (CandidateResult) results.get(0);
        assertEquals(List.of(4, 3, 2), List.of(president.votesFor("C1"), president.votesFor("C2"),
                president.votesFor("C3")));
        assertEquals(List.of("C1"), winners(president));

        CandidateResult committee = (CandidateResult) results.get(1);
        assertEquals(List.of(5, 5, 3, 3), List.of(committee.votesFor("C4"), committee.votesFor("C5"),
                committee.votesFor("C6"), committee.votesFor("C7")));
        assertEquals(List.of("C4", "C5"), winners(committee));                  // tied, but both fit the 2 seats

        YesNoResult fee = (YesNoResult) results.get(2);
        assertEquals(6, fee.getYes());
        assertEquals(3, fee.getNo());
        assertTrue(fee.isPassed());
        assertEquals("Passed (66.7% yes)", fee.summary().lines().toList().get(1));
    }

    @Test
    void openElectionsHaveNoResultsYet() {
        for (Election open : elections.findByStatus(ElectionStatus.OPEN)) {
            assertFalse(tally.closedElections().contains(open));
        }
    }
}
