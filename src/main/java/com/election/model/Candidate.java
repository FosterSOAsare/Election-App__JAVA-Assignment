package com.election.model;

import com.election.exception.InvalidNameException;

public class Candidate {

    private final String id;
    // Not final: a draft election's candidates can be renamed, through Category.renameCandidate.
    private String name;
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

    /**
     * Package-private on purpose: only Category calls this, after checking that no other candidate
     * in the category already has the new name.
     */
    void rename(String newName) throws InvalidNameException {
        NameRules.requireValidName(newName, "Candidate name");
        this.name = newName;
    }

    public String getElectionId() {
        return electionId;
    }

    public String getCategoryId() {
        return categoryId;
    }
}
