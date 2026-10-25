package com.election.repository;

import com.election.model.Administrator;
import com.election.model.User;
import com.election.model.Voter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserRepositoryTest {

    private final UserRepository users = new UserRepository(List.of(new Administrator("A1"), new Voter("V001")));

    @Test
    void aUserIsFoundByTheirId() {
        User found = users.findById("A1").orElseThrow();
        assertEquals("A1", found.getId());
        assertTrue(found.canManageElections());
    }

    @Test
    void theIdMayBeTypedInAnyCase() {
        assertEquals("V001", users.findById("v001").orElseThrow().getId());
    }

    @Test
    void anUnknownIdFindsNoOne() {
        assertTrue(users.findById("V099").isEmpty());
        assertTrue(users.findById("").isEmpty());
    }

    @Test
    void theSameIdTwiceIsRefusedEvenInAnotherCase() {
        assertThrows(IllegalArgumentException.class,
                () -> new UserRepository(List.of(new Voter("V001"), new Voter("v001"))));
    }

    @Test
    void anEmptyListGivesAnEmptyRepository() {
        assertTrue(new UserRepository(List.of()).findById("A1").isEmpty());
    }
}
