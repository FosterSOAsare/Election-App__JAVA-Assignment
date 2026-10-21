package com.election.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Category {

    private final String id;
    private final String electionId;
    private final String name;
    private final BallotType ballotType;
    // Number of winners: always 1 for SELECT_ONE and YES_NO, at least 2 for SELECT_MULTIPLE.
    private final int seats;

    // LinkedHashMap keeps the candidates in the order they were added.
    private final Map<String, Candidate> candidatesById = new LinkedHashMap<>();

    public Category(String id, String electionId, String name, BallotType ballotType, int seats) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Category id must not be blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Category name must not be blank");
        }
        if (ballotType == null) {
            throw new IllegalArgumentException("Category " + name + " needs a ballot type");
        }
        if (ballotType == BallotType.SELECT_MULTIPLE && seats < 2) {
            throw new IllegalArgumentException("Category " + name + " is SELECT_MULTIPLE, so it needs at least 2 seats");
        }
        if (ballotType != BallotType.SELECT_MULTIPLE && seats != 1) {
            throw new IllegalArgumentException("Category " + name + " is " + ballotType + ", so it has exactly 1 seat");
        }
        this.id = id;
        this.electionId = electionId;
        this.name = name;
        this.ballotType = ballotType;
        this.seats = seats;
    }

    public String getId() {
        return id;
    }

    public String getElectionId() {
        return electionId;
    }

    public String getName() {
        return name;
    }

    public BallotType getBallotType() {
        return ballotType;
    }

    public int getSeats() {
        return seats;
    }

    public List<Candidate> getCandidates() {
        return List.copyOf(candidatesById.values());
    }

    public void addCandidate(Candidate candidate) {
        if (ballotType == BallotType.YES_NO) {
            throw new IllegalArgumentException("Category " + name + " is a yes/no question, so it has no candidates");
        }
        if (!candidate.getElectionId().equals(electionId) || !candidate.getCategoryId().equals(id)) {
            throw new IllegalArgumentException("Candidate " + candidate.getId() + " belongs to another category");
        }
        if (candidatesById.containsKey(candidate.getId())) {
            throw new IllegalArgumentException("Duplicate candidate id " + candidate.getId() + " in " + name);
        }
        boolean nameTaken = candidatesById.values().stream()
                .anyMatch(c -> c.getName().equalsIgnoreCase(candidate.getName()));
        if (nameTaken) {
            throw new IllegalArgumentException("Candidate " + candidate.getName() + " is already standing for " + name);
        }
        candidatesById.put(candidate.getId(), candidate);
    }
}
