package com.election;

import com.election.exception.InvalidNameException;
import com.election.model.Administrator;
import com.election.model.BallotType;
import com.election.model.Candidate;
import com.election.model.Category;
import com.election.model.Deadlines;
import com.election.model.Election;
import com.election.model.ElectionStatus;
import com.election.model.Voter;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/** Small building blocks the tests share, so every test still shows what it starts from. */
public final class TestData {

    /** Far enough ahead that it never passes while the tests are being written or marked. */
    public static final LocalDateTime FUTURE = Deadlines.parse("2030-01-01");

    private TestData() {
    }

    public static Administrator admin() {
        return new Administrator("A1");
    }

    public static Voter voter() {
        return new Voter("V001");
    }

    /** An empty draft created by A1. */
    public static Election draft(String id) {
        return election(id, ElectionStatus.DRAFT);
    }

    public static Election election(String id, ElectionStatus status) {
        return new Election(id, "Election " + id, FUTURE, status, "A1");
    }

    /** A draft whose CAT1 President (Select one) has C1 Ana and C2 Ben. */
    public static Election draftWithPresident(String id) throws InvalidNameException {
        Election election = draft(id);
        Category president = category(election, "CAT1", "President", BallotType.SELECT_ONE, 1);
        addCandidate(president, "C1", "Ana");
        addCandidate(president, "C2", "Ben");
        return election;
    }

    /** Adds a new category to the election and returns it. */
    public static Category category(Election election, String id, String name, BallotType type, int seats) {
        Category category = new Category(id, election.getId(), name, type, seats);
        election.addCategory(category);
        return category;
    }

    public static Candidate addCandidate(Category category, String id, String name) throws InvalidNameException {
        Candidate candidate = new Candidate(id, name, category.getElectionId(), category.getId());
        category.addCandidate(candidate);
        return candidate;
    }

    /** A clock stopped at the given moment, read as UTC, e.g. "2026-11-10T17:59:59". */
    public static Clock clockAt(String localDateTime) {
        return Clock.fixed(Instant.parse(localDateTime + "Z"), ZoneOffset.UTC);
    }
}
