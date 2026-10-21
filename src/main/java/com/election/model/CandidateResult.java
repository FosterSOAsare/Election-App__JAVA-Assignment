package com.election.model;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Result of a Select one or Select multiple category: who was elected, and who is tied for the last seat. */
public class CandidateResult extends CategoryResult {

    private final List<Candidate> winners;
    private final List<Candidate> tiedForLastSeat;

    public CandidateResult(Category category, int ballotCount, Map<String, Integer> tallies,
                           List<Candidate> winners, List<Candidate> tiedForLastSeat) {
        super(category, ballotCount, tallies);
        this.winners = List.copyOf(winners);
        this.tiedForLastSeat = List.copyOf(tiedForLastSeat);
    }

    public List<Candidate> getWinners() {
        return winners;
    }

    public List<Candidate> getTiedForLastSeat() {
        return tiedForLastSeat;
    }

    @Override
    public String summary() {
        // Most votes first; candidates with equal votes keep their file order.
        List<Candidate> ranked = getCategory().getCandidates().stream()
                .sorted(Comparator.comparingInt((Candidate c) -> votesFor(c.getId())).reversed())
                .toList();

        int idWidth = ranked.stream().mapToInt(c -> c.getId().length()).max().orElse(0);
        int nameWidth = ranked.stream().mapToInt(c -> c.getName().length()).max().orElse(0);
        String row = "%-" + idWidth + "s  %-" + nameWidth + "s  %d%n";

        StringBuilder text = new StringBuilder();
        for (Candidate candidate : ranked) {
            text.append(String.format(row, candidate.getId(), candidate.getName(), votesFor(candidate.getId())));
        }

        if (winners.isEmpty() && tiedForLastSeat.isEmpty()) {
            return text.append("No one elected (no votes)").toString();
        }
        if (!winners.isEmpty()) {
            text.append("Elected: ").append(names(winners));
        }
        if (!tiedForLastSeat.isEmpty()) {
            if (!winners.isEmpty()) {
                text.append(System.lineSeparator());
            }
            int undecided = getCategory().getSeats() - winners.size();
            text.append("Tie: ").append(names(tiedForLastSeat))
                    .append(undecided == 1 ? ", seat undecided" : ", " + undecided + " seats undecided");
        }
        return text.toString();
    }

    private static String names(List<Candidate> candidates) {
        return candidates.stream().map(Candidate::getName).collect(Collectors.joining(", "));
    }
}
