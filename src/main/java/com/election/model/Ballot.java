package com.election.model;

import com.election.exception.InvalidBallotException;

import java.util.List;

/**
 * One voter's answer in one category: candidate ids (C1, or C4 and C5) or YES / NO.
 * Each ballot type is a subclass that knows its own rules: validate(Category) checks the choices
 * and stores them exactly as the category spells them, so "c1" becomes C1 and "yes" becomes YES.
 * BallotFactory picks the subclass from the category's ballot type.
 */
public abstract class Ballot {

    private final String voterId;
    private final String electionId;
    private final String categoryId;
    // Not final: validate replaces what was typed with the exact ids.
    private List<String> choices;

    protected Ballot(String voterId, String electionId, String categoryId, List<String> choices) {
        this.voterId = voterId;
        this.electionId = electionId;
        this.categoryId = categoryId;
        this.choices = List.copyOf(choices);
    }

    /** Checks the choices against the category; throws if they do not fit this ballot type. */
    public abstract void validate(Category category) throws InvalidBallotException;

    /** For subclasses: keep the checked choices in their exact form. */
    protected void useChoices(List<String> exactChoices) {
        this.choices = List.copyOf(exactChoices);
    }

    /** Finds the candidate in this category whatever case the id was typed in, so "c1" means C1. */
    protected static String candidateId(Category category, String typedId) throws InvalidBallotException {
        return category.getCandidates().stream()
                .map(Candidate::getId)
                .filter(id -> id.equalsIgnoreCase(typedId))
                .findFirst()
                .orElseThrow(() -> new InvalidBallotException(typedId + " is not a candidate for "
                        + category.getName()));
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
