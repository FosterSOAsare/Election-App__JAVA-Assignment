package com.election.model;

public class Voter extends User {

    public Voter(String id) {
        super(id);
    }

    @Override
    public boolean canManageElections() {
        return false;
    }

    @Override
    public boolean canVote() {
        return true;
    }
}
