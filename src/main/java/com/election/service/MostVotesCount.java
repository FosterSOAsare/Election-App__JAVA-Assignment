package com.election.service;

import com.election.model.Ballot;
import com.election.model.Candidate;
import com.election.model.CandidateResult;
import com.election.model.Category;
import com.election.model.CategoryResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Select one and Select multiple: every choice is one vote, and the top "seats" candidates win.
 * If candidates tie for the last seat and there is not room for all of them, that seat stays undecided.
 */
public class MostVotesCount implements CountingMethod {

    @Override
    public CategoryResult count(Category category, List<Ballot> ballots) {
        // Start every candidate at 0 so candidates without votes still show up.
        Map<String, Integer> tallies = new LinkedHashMap<>();
        for (Candidate candidate : category.getCandidates()) {
            tallies.put(candidate.getId(), 0);
        }
        for (Ballot ballot : ballots) {
            for (String choice : ballot.getChoices()) {
                // Only real candidates of this category are counted.
                tallies.computeIfPresent(choice, (id, votes) -> votes + 1);
            }
        }

        List<Candidate> ranked = category.getCandidates().stream()
                .sorted(Comparator.comparingInt((Candidate c) -> tallies.get(c.getId())).reversed())
                .toList();
        if (ranked.isEmpty() || tallies.get(ranked.get(0).getId()) == 0) {
            return new CandidateResult(category, ballots.size(), tallies, List.of(), List.of());
        }

        // Whoever is in the last winning place decides who is safely elected and who is tied.
        int seats = category.getSeats();
        int lastSeatVotes = tallies.get(ranked.get(Math.min(seats, ranked.size()) - 1).getId());

        List<Candidate> winners = new ArrayList<>();
        List<Candidate> onLastSeatVotes = new ArrayList<>();
        for (Candidate candidate : ranked) {
            int votes = tallies.get(candidate.getId());
            if (votes > lastSeatVotes) {
                winners.add(candidate);
            } else if (votes == lastSeatVotes && votes > 0) {
                onLastSeatVotes.add(candidate);
            }
        }

        int seatsLeft = seats - winners.size();
        if (onLastSeatVotes.size() <= seatsLeft) {
            // Room for everyone on that score, so the tie does not matter (like C4 and C5 in E1).
            winners.addAll(onLastSeatVotes);
            return new CandidateResult(category, ballots.size(), tallies, winners, List.of());
        }
        return new CandidateResult(category, ballots.size(), tallies, winners, onLastSeatVotes);
    }
}
