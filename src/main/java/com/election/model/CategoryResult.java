package com.election.model;

import java.util.LinkedHashMap;
import java.util.Map;

/** The counted outcome of one category. Each subclass describes itself through summary(). */
public abstract class CategoryResult {

    private final Category category;
    private final int ballotCount;
    // Option id (a candidate id, or YES / NO) to number of votes.
    private final Map<String, Integer> tallies;

    protected CategoryResult(Category category, int ballotCount, Map<String, Integer> tallies) {
        this.category = category;
        this.ballotCount = ballotCount;
        this.tallies = new LinkedHashMap<>(tallies);
    }

    public Category getCategory() {
        return category;
    }

    public int getBallotCount() {
        return ballotCount;
    }

    public int votesFor(String optionId) {
        return tallies.getOrDefault(optionId, 0);
    }

    /** The tally and the outcome as plain lines of text, without indentation. */
    public abstract String summary();
}
