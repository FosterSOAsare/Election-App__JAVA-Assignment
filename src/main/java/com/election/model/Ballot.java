package com.election.model;

import java.util.List;

/**
 * One voter's answer in one category: candidate ids (C1, C4|C5) or YES / NO.
 * Not checked against the voting rules yet; that comes with counting.
 */
public class Ballot {

    private final String voterId;
    private final String electionId;
    private final String categoryId;
    private final List<String> choices;

    public Ballot(String voterId, String electionId, String categoryId, List<String> choices) {
        this.voterId = voterId;
        this.electionId = electionId;
        this.categoryId = categoryId;
        this.choices = List.copyOf(choices);
    }

    public String getVoterId() {
        return voterId;
    }

    public String getElectionId() {
        return electionId;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public List<String> getChoices() {
        return choices;
    }
}
