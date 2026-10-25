package com.election.model;

import com.election.exception.InvalidNameException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** What a Select one / Select multiple result says about itself; who wins is MostVotesCount's job. */
class CandidateResultTest {

    private Category committee;
    private Candidate ana;
    private Candidate ben;
    private Candidate cara;

    @BeforeEach
    void setUp() throws InvalidNameException {
        committee = new Category("CAT2", "E1", "Committee", BallotType.SELECT_MULTIPLE, 2);
        ana = new Candidate("C1", "Ana", "E1", "CAT2");
        ben = new Candidate("C2", "Ben", "E1", "CAT2");
        cara = new Candidate("C3", "Cara", "E1", "CAT2");
        committee.addCandidate(ana);
        committee.addCandidate(ben);
        committee.addCandidate(cara);
    }

    private static Map<String, Integer> tallies(int ana, int ben, int cara) {
        Map<String, Integer> tallies = new LinkedHashMap<>();
        tallies.put("C1", ana);
        tallies.put("C2", ben);
        tallies.put("C3", cara);
        return tallies;
    }

    @Test
    void candidatesAreListedMostVotesFirstThenTheWinners() {
        CandidateResult result = new CandidateResult(committee, 6, tallies(2, 5, 4), List.of(ben, cara), List.of());

        assertEquals(List.of(
                "C2  Ben   5",
                "C3  Cara  4",
                "C1  Ana   2",
                "Elected: Ben, Cara"), result.summary().lines().toList());
        assertEquals(List.of(ben, cara), result.getWinners());
    }

    @Test
    void candidatesWithEqualVotesKeepTheirOrder() {
        CandidateResult result = new CandidateResult(committee, 3, tallies(3, 3, 1), List.of(ana, ben), List.of());

        assertEquals(List.of("C1  Ana   3", "C2  Ben   3", "C3  Cara  1"),
                result.summary().lines().limit(3).toList());
    }

    @Test
    void aTieForTheLastSeatIsShownAsUndecided() {
        CandidateResult result = new CandidateResult(committee, 5, tallies(4, 2, 2), List.of(ana), List.of(ben, cara));

        List<String> lines = result.summary().lines().toList();
        assertEquals("Elected: Ana", lines.get(3));
        assertEquals("Tie: Ben, Cara, seat undecided", lines.get(4));
        assertEquals(List.of(ben, cara), result.getTiedForLastSeat());
    }

    @Test
    void aTieForEverySeatSaysHowManyAreUndecided() {
        CandidateResult result = new CandidateResult(committee, 3, tallies(1, 1, 1), List.of(), List.of(ana, ben, cara));

        List<String> lines = result.summary().lines().toList();
        assertEquals("Tie: Ana, Ben, Cara, 2 seats undecided", lines.get(lines.size() - 1));
    }

    @Test
    void noVotesElectsNoOne() {
        CandidateResult result = new CandidateResult(committee, 0, tallies(0, 0, 0), List.of(), List.of());

        List<String> lines = result.summary().lines().toList();
        assertEquals("No one elected (no votes)", lines.get(lines.size() - 1));
        assertEquals(0, result.votesFor("C1"));
    }
}
