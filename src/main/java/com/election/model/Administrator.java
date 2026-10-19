package com.election.model;

/** Creates and runs elections; never votes. */
public class Administrator extends User {

    public Administrator(String id) {
        super(id);
    }

    @Override
    public boolean canManageElections() {
        return true;
    }

    @Override
    public boolean canVote() {
        return false;
    }
}
