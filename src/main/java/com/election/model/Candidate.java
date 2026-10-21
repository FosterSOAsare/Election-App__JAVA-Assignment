package com.election.model;

import com.election.exception.InvalidNameException;

public class Candidate {

    private final String id;
    private final String name;
    private final String electionId;
    private final String categoryId;

    public Candidate(String id, String name, String electionId, String categoryId) throws InvalidNameException {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Candidate id must not be blank");
        }
        NameRules.requireValidName(name, "Candidate name");
        this.id = id;
        this.name = name;
        this.electionId = electionId;
        this.categoryId = categoryId;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getElectionId() {
        return electionId;
    }

    public String getCategoryId() {
        return categoryId;
    }
}
