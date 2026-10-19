package com.election.model;

public abstract class User implements ManagementPermission, VotingPermission {

    private final String id;

    protected User(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("User id must not be blank");
        }
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
