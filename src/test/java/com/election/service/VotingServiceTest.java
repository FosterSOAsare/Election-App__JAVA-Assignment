package com.election.service;

import com.election.TestData;
import com.election.exception.DuplicateVoteException;
import com.election.exception.ElectionStateException;
import com.election.exception.IneligibleVoterException;
import com.election.exception.InvalidBallotException;
import com.election.model.Ballot;
import com.election.model.BallotType;
import com.election.model.Category;
import com.election.model.Deadlines;
import com.election.model.Election;
import com.election.model.ElectionStatus;
import com.election.model.SingleChoiceBallot;
import com.election.model.User;
import com.election.model.Voter;
import com.election.repository.ElectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VotingServiceTest {

    private static final String DEADLINE = "2026-11-10 18:00";
    private static final Clock BEFORE_DEADLINE = TestData.clockAt("2026-11-01T12:00:00");

    private final User voter = TestData.voter();
    private Election election;
    private ElectionRepository repository;

    /**
     * An OPEN election E1 with an 18:00 deadline:
     * CAT1 President (Select one: C1 Ana, C2 Ben), CAT2 Committee (Select multiple, 2 seats: C3, C4, C5),
     * CAT3 a Yes / No question.
     */
    @BeforeEach
    void setUp() throws Exception {
        election = new Election("E1", "Student Council", Deadlines.parse(DEADLINE), ElectionStatus.DRAFT, "A1");
        Category president = TestData.category(election, "CAT1", "President", BallotType.SELECT_ONE, 1);
        TestData.addCandidate(president, "C1", "Ana");
        TestData.addCandidate(president, "C2", "Ben");
        Category committee = TestData.category(election, "CAT2", "Committee", BallotType.SELECT_MULTIPLE, 2);
        TestData.addCandidate(committee, "C3", "Cara");
        TestData.addCandidate(committee, "C4", "Dan");
        TestData.addCandidate(committee, "C5", "Eve");
        TestData.category(election, "CAT3", "Raise the fee?", BallotType.YES_NO, 1);
        election.open();
        repository = new ElectionRepository(List.of(election));
    }

    private VotingService votingAt(Clock clock) {
        return new VotingService(repository, clock, new BallotFactory());
    }

    private VotingService voting() {
        return votingAt(BEFORE_DEADLINE);
    }

    @Nested
    class HappyPaths {

        @Test
        void aVoterAnswersEveryCategoryAndAllBallotsAreStoredTogether() throws Exception {
            VotingService voting = voting();
            List<Ballot> ballots = List.of(
                    voting.prepareBallot(voter, election, "CAT1", List.of("c1")),
                    voting.prepareBallot(voter, election, "CAT2", List.of("C3", "c5")),
                    voting.prepareBallot(voter, election, "CAT3", List.of("yes")));

            voting.submitBallots(voter, election, ballots);

            assertEquals(3, election.getBallots().size());
            assertEquals(List.of("C1"), election.getBallots().get(0).getChoices());
            assertEquals(List.of("C3", "C5"), election.getBallots().get(1).getChoices());
            assertEquals(List.of("YES"), election.getBallots().get(2).getChoices());
            assertTrue(voting.hasVoted(voter, election));
        }

        @Test
        void preparingABallotDoesNotStoreIt() throws Exception {
            voting().prepareBallot(voter, election, "CAT1", List.of("C1"));

            assertTrue(election.getBallots().isEmpty());
            assertFalse(voting().hasVoted(voter, election));
        }

        @Test
        void aVoterMaySkipCategories() throws Exception {
            VotingService voting = voting();
            voting.submitBallots(voter, election, List.of(voting.prepareBallot(voter, election, "CAT3", List.of("NO"))));

            assertEquals(1, election.getBallots().size());
        }

        @Test
        void otherVotersCanStillVoteAfterSomeoneHas() throws Exception {
            VotingService voting = voting();
            voting.submitBallots(voter, election, List.of(voting.prepareBallot(voter, election, "CAT1", List.of("C1"))));

            Voter other = new Voter("V002");
            assertFalse(voting.hasVoted(other, election));
            assertDoesNotThrow(() -> voting.submitBallots(other, election,
                    List.of(voting.prepareBallot(other, election, "CAT1", List.of("C2")))));
            assertEquals(2, election.getBallots().size());
        }

        @Test
        void hasVotedIgnoresTheCaseOfTheVoterId() throws Exception {
            election.addBallot(new SingleChoiceBallot("v001", "E1", "CAT1", List.of("C1")));
            assertTrue(voting().hasVoted(voter, election));
        }
    }

    @Nested
    class WhoMayVote {

        @Test
        void anAdministratorIsRefused() {
            assertThrows(IneligibleVoterException.class,
                    () -> voting().prepareBallot(TestData.admin(), election, "CAT1", List.of("C1")));
        }

        @Test
        void aSecondSubmissionInTheSameElectionIsRefused() throws Exception {
            VotingService voting = voting();
            voting.submitBallots(voter, election, List.of(voting.prepareBallot(voter, election, "CAT1", List.of("C1"))));

            assertThrows(DuplicateVoteException.class,
                    () -> voting.prepareBallot(voter, election, "CAT2", List.of("C3")));
        }
    }

    @Nested
    class WhenVotingIsAllowed {

        @Test
        void aDraftOrClosedElectionIsRefused() throws Exception {
            Election draft = TestData.draftWithPresident("E2");
            assertThrows(ElectionStateException.class,
                    () -> voting().prepareBallot(voter, draft, "CAT1", List.of("C1")));

            election.close();
            assertThrows(ElectionStateException.class,
                    () -> voting().prepareBallot(voter, election, "CAT1", List.of("C1")));
        }

        @Test
        void oneSecondBeforeTheDeadlineIsAccepted() {
            VotingService voting = votingAt(TestData.clockAt("2026-11-10T17:59:59"));
            assertDoesNotThrow(() -> voting.prepareBallot(voter, election, "CAT1", List.of("C1")));
            assertFalse(voting.deadlinePassed(election));
        }

        @Test
        void fromTheDeadlineItselfVotesAreRefused() {
            VotingService voting = votingAt(TestData.clockAt("2026-11-10T18:00:00"));

            ElectionStateException e = assertThrows(ElectionStateException.class,
                    () -> voting.prepareBallot(voter, election, "CAT1", List.of("C1")));
            assertEquals("Voting in Student Council ended at 2026-11-10 18:00:00", e.getMessage());
            assertTrue(voting.deadlinePassed(election));
        }

        @Test
        void openElectionsHidesElectionsWhoseDeadlineHasPassed() {
            assertEquals(List.of(election), votingAt(TestData.clockAt("2026-11-10T17:59:59")).openElections());
            assertTrue(votingAt(TestData.clockAt("2026-11-10T18:00:00")).openElections().isEmpty());
        }

        @Test
        void openElectionsLeavesOutDraftsAndClosedElections() {
            repository.add(TestData.draft("E2"));
            repository.add(TestData.election("E3", ElectionStatus.CLOSED));

            assertEquals(List.of(election), voting().openElections());
        }
    }

    @Nested
    class WhatCanBeChosen {

        @Test
        void anUnknownCategoryIsRefused() {
            InvalidBallotException e = assertThrows(InvalidBallotException.class,
                    () -> voting().prepareBallot(voter, election, "VP", List.of("C1")));
            assertEquals("There is no category VP in E1", e.getMessage());
        }

        @Test
        void choicesThatDoNotFitTheCategoryAreRefused() {
            VotingService voting = voting();
            assertThrows(InvalidBallotException.class,
                    () -> voting.prepareBallot(voter, election, "CAT1", List.of("C3")));
            assertThrows(InvalidBallotException.class,
                    () -> voting.prepareBallot(voter, election, "CAT1", List.of("C1", "C2")));
            assertThrows(InvalidBallotException.class,
                    () -> voting.prepareBallot(voter, election, "CAT2", List.of("C3", "C4", "C5")));
            assertThrows(InvalidBallotException.class,
                    () -> voting.prepareBallot(voter, election, "CAT2", List.of("C3", "C3")));
            assertThrows(InvalidBallotException.class,
                    () -> voting.prepareBallot(voter, election, "CAT3", List.of("MAYBE")));
        }
    }

    @Nested
    class AllOrNothing {

        @Test
        void twoBallotsForTheSameCategoryStoreNothing() throws Exception {
            VotingService voting = voting();
            Ballot first = voting.prepareBallot(voter, election, "CAT1", List.of("C1"));
            Ballot second = voting.prepareBallot(voter, election, "CAT1", List.of("C2"));

            assertThrows(DuplicateVoteException.class,
                    () -> voting.submitBallots(voter, election, List.of(first, second)));
            assertTrue(election.getBallots().isEmpty());
        }

        @Test
        void oneBadBallotStoresNothing() throws Exception {
            VotingService voting = voting();
            Ballot good = voting.prepareBallot(voter, election, "CAT1", List.of("C1"));
            Ballot bad = new SingleChoiceBallot("V001", "E1", "CAT2", List.of("C1"));   // C1 is not a committee candidate

            assertThrows(InvalidBallotException.class, () -> voting.submitBallots(voter, election, List.of(good, bad)));
            assertTrue(election.getBallots().isEmpty());
            assertFalse(voting.hasVoted(voter, election));
        }

        @Test
        void ballotsPreparedBeforeTheDeadlineAreRefusedIfSubmittedAfterIt() throws Exception {
            Ballot prepared = votingAt(TestData.clockAt("2026-11-10T17:59:59"))
                    .prepareBallot(voter, election, "CAT1", List.of("C1"));

            VotingService tooLate = votingAt(TestData.clockAt("2026-11-10T18:00:00"));
            assertThrows(ElectionStateException.class, () -> tooLate.submitBallots(voter, election, List.of(prepared)));
            assertTrue(election.getBallots().isEmpty());
        }
    }
}
