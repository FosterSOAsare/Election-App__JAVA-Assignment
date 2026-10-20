package com.election.model;

public class Category {

    private final String id;
    private final String electionId;
    private final String name;

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
}
