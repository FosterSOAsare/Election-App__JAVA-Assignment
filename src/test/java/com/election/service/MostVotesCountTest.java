package com.election.service;

import com.election.TestData;
import com.election.model.Ballot;
import com.election.model.BallotType;
import com.election.model.Candidate;
import com.election.model.CandidateResult;
import com.election.model.Category;
import com.election.model.Election;
import com.election.model.MultipleChoiceBallot;
import com.election.model.SingleChoiceBallot;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MostVotesCountTest {

    private final MostVotesCount counting = new MostVotesCount();

    /** A category with candidates C1, C2, ... named Ana, Ben, Cara, Dan, Eve. */
    private static Category category(BallotType type, int seats, int candidates) throws Exception {
        Election election = TestData.draft("E1");
        Category category = TestData.category(election, "CAT1", "Committee", type, seats);
        String[] names = {"Ana", "Ben", "Cara", "Dan", "Eve"};
        for (int i = 0; i < candidates; i++) {
            TestData.addCandidate(category, "C" + (i + 1), names[i]);
        }
        return category;
    }

    /** "C1:5, C2:3" means five Select one ballots for C1 and three for C2. */
    private static List<Ballot> singleVotes(String... idTimesCount) {
        List<Ballot> ballots = new ArrayList<>();
        for (String entry : idTimesCount) {
            String[] parts = entry.split(":");
            for (int i = 0; i < Integer.parseInt(parts[1]); i++) {
                ballots.add(new SingleChoiceBallot("V" + ballots.size(), "E1", "CAT1", List.of(parts[0])));
            }
        }
        return ballots;
    }

    private static Ballot multi(String... ids) {
        return new MultipleChoiceBallot("V", "E1", "CAT1", List.of(ids));
    }

    private static List<String> ids(List<Candidate> candidates) {
        return candidates.stream().map(Candidate::getId).toList();
    }

    // --- happy paths

    @Test
    void oneSeatGoesToTheMostVotes() throws Exception {
        // Seed E3 President: C1 5, C2 3, C3 2.
        Category president = category(BallotType.SELECT_ONE, 1, 3);
        CandidateResult result = (CandidateResult) counting.count(president, singleVotes("C1:5", "C2:3", "C3:2"));

        assertEquals(List.of("C1"), ids(result.getWinners()));
        assertTrue(result.getTiedForLastSeat().isEmpty());
        assertEquals(5, result.votesFor("C1"));
        assertEquals(10, result.getBallotCount());
    }

    @Test
    void severalSeatsGoToTheTopCandidates() throws Exception {
        // Seed E4 Library committee, 3 seats: C1 7, C2 6, C3 5, C4 4, C5 3.
        Category committee = category(BallotType.SELECT_MULTIPLE, 3, 5);
        List<Ballot> ballots = List.of(
                multi("C1", "C2", "C3"), multi("C1", "C2", "C4"), multi("C1", "C3", "C5"), multi("C1", "C2"),
                multi("C2", "C3", "C4"), multi("C1", "C4", "C5"), multi("C2", "C3"), multi("C1", "C5"),
                multi("C3", "C4"), multi("C1", "C2"));

        CandidateResult result = (CandidateResult) counting.count(committee, ballots);

        assertEquals(List.of("C1", "C2", "C3"), ids(result.getWinners()));
        assertEquals(List.of(7, 6, 5, 4, 3), List.of(result.votesFor("C1"), result.votesFor("C2"),
                result.votesFor("C3"), result.votesFor("C4"), result.votesFor("C5")));
    }

    @Test
    void aTieThatStillFitsTheSeatsIsNoTie() throws Exception {
        // Seed E1 Committee, 2 seats: C1 and C2 both on 5, the others on 3.
        Category committee = category(BallotType.SELECT_MULTIPLE, 2, 4);
        List<Ballot> ballots = List.of(multi("C1", "C2"), multi("C1", "C3"), multi("C2"), multi("C1", "C4"),
                multi("C2", "C3"), multi("C1", "C2"), multi("C3", "C4"), multi("C1"), multi("C2", "C4"));

        CandidateResult result = (CandidateResult) counting.count(committee, ballots);

        assertEquals(List.of("C1", "C2"), ids(result.getWinners()));
        assertTrue(result.getTiedForLastSeat().isEmpty());
    }

    // --- ties at the last seat

    @Test
    void aTieForTheOnlySeatLeavesItUndecided() throws Exception {
        // Seed E3 Treasurer: 4-4.
        Category treasurer = category(BallotType.SELECT_ONE, 1, 2);
        CandidateResult result = (CandidateResult) counting.count(treasurer, singleVotes("C1:4", "C2:4"));

        assertTrue(result.getWinners().isEmpty());
        assertEquals(List.of("C1", "C2"), ids(result.getTiedForLastSeat()));
    }

    @Test
    void aTieForTheLastOfSeveralSeatsElectsTheOthers() throws Exception {
        // 2 seats: C1 3, then C2 and C3 tied on 2 for the second seat.
        Category committee = category(BallotType.SELECT_MULTIPLE, 2, 3);
        List<Ballot> ballots = List.of(multi("C1", "C2"), multi("C1", "C3"), multi("C1"), multi("C2"), multi("C3"));

        CandidateResult result = (CandidateResult) counting.count(committee, ballots);

        assertEquals(List.of("C1"), ids(result.getWinners()));
        assertEquals(List.of("C2", "C3"), ids(result.getTiedForLastSeat()));
    }

    @Test
    void aThreeWayTieForOneSeatListsAllThree() throws Exception {
        Category president = category(BallotType.SELECT_ONE, 1, 3);
        CandidateResult result = (CandidateResult) counting.count(president, singleVotes("C1:2", "C2:2", "C3:2"));

        assertTrue(result.getWinners().isEmpty());
        assertEquals(List.of("C1", "C2", "C3"), ids(result.getTiedForLastSeat()));
    }

    // --- edge cases

    @Test
    void noBallotsElectsNoOne() throws Exception {
        Category president = category(BallotType.SELECT_ONE, 1, 3);
        CandidateResult result = (CandidateResult) counting.count(president, List.of());

        assertTrue(result.getWinners().isEmpty());
        assertTrue(result.getTiedForLastSeat().isEmpty());
        assertEquals(0, result.getBallotCount());
        assertEquals(0, result.votesFor("C1"));
    }

    @Test
    void candidatesWithoutAnyVoteAreNeverElectedEvenWithSeatsFree() throws Exception {
        // 2 seats but only C1 got votes: the second seat stays empty, it is not shared by the zero-vote candidates.
        Category committee = category(BallotType.SELECT_MULTIPLE, 2, 3);
        CandidateResult result = (CandidateResult) counting.count(committee, List.of(multi("C1"), multi("C1")));

        assertEquals(List.of("C1"), ids(result.getWinners()));
        assertTrue(result.getTiedForLastSeat().isEmpty());
    }

    @Test
    void moreSeatsThanCandidatesElectsEveryoneWithVotes() throws Exception {
        Category committee = category(BallotType.SELECT_MULTIPLE, 3, 2);
        CandidateResult result = (CandidateResult) counting.count(committee, List.of(multi("C1", "C2"), multi("C1")));

        assertEquals(List.of("C1", "C2"), ids(result.getWinners()));
        assertTrue(result.getTiedForLastSeat().isEmpty());
    }

    @Test
    void choicesThatAreNotCandidatesOfTheCategoryAreNotCounted() throws Exception {
        Category president = category(BallotType.SELECT_ONE, 1, 2);
        CandidateResult result = (CandidateResult) counting.count(president, singleVotes("C1:1", "C99:5"));

        assertEquals(List.of("C1"), ids(result.getWinners()));
        assertEquals(0, result.votesFor("C99"));
        assertEquals(6, result.getBallotCount());
    }
}
