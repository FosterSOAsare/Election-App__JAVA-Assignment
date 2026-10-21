package com.election.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Category {

    private final String id;
    private final String electionId;
    private final String name;

    // LinkedHashMap keeps the candidates in the order they were added.
    private final Map<String, Candidate> candidatesById = new LinkedHashMap<>();

    public Category(String id, String electionId, String name) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Category id must not be blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Category name must not be blank");
        }
        this.id = id;
        this.electionId = electionId;
        this.name = name;
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

    public List<Candidate> getCandidates() {
        return List.copyOf(candidatesById.values());
    }

    public void addCandidate(Candidate candidate) {
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
