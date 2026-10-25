package com.election.model;

import com.election.exception.InvalidNameException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CandidateTest {

    @Test
    void aValidCandidateKeepsItsDetails() throws InvalidNameException {
        Candidate candidate = new Candidate("C1", "Amara Mensah", "E1", "CAT1");

        assertEquals("C1", candidate.getId());
        assertEquals("Amara Mensah", candidate.getName());
        assertEquals("E1", candidate.getElectionId());
        assertEquals("CAT1", candidate.getCategoryId());
    }

    @Test
    void anInvalidNameIsRefused() {
        assertThrows(InvalidNameException.class, () -> new Candidate("C1", "Zoe#1", "E1", "CAT1"));
    }

    @Test
    void aMissingNameIsRefused() {
        assertThrows(InvalidNameException.class, () -> new Candidate("C1", null, "E1", "CAT1"));
        assertThrows(InvalidNameException.class, () -> new Candidate("C1", "", "E1", "CAT1"));
    }

    @Test
    void aMissingOrBlankIdIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> new Candidate(null, "Ana", "E1", "CAT1"));
        assertThrows(IllegalArgumentException.class, () -> new Candidate(" ", "Ana", "E1", "CAT1"));
    }
}
