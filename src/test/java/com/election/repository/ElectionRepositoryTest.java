package com.election.repository;

import com.election.TestData;
import com.election.model.Election;
import com.election.model.ElectionStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ElectionRepositoryTest {

    private static List<String> ids(List<Election> elections) {
        return elections.stream().map(Election::getId).toList();
    }

    @Test
    void electionsStayInTheOrderTheyWereLoaded() {
        ElectionRepository repository = new ElectionRepository(List.of(
                TestData.draft("E3"), TestData.draft("E1"), TestData.draft("E2")));

        assertEquals(List.of("E3", "E1", "E2"), ids(repository.findAll()));
    }

    @Test
    void anElectionIsFoundByItsIdInAnyCase() {
        Election e1 = TestData.draft("E1");
        ElectionRepository repository = new ElectionRepository(List.of(e1));

        assertSame(e1, repository.findById("e1").orElseThrow());
        assertTrue(repository.findById("E9").isEmpty());
    }

    @Test
    void findByStatusReturnsOnlyThatStatusInOrder() {
        ElectionRepository repository = new ElectionRepository(List.of(
                TestData.election("E1", ElectionStatus.OPEN),
                TestData.election("E2", ElectionStatus.DRAFT),
                TestData.election("E3", ElectionStatus.CLOSED),
                TestData.election("E4", ElectionStatus.OPEN)));

        assertEquals(List.of("E1", "E4"), ids(repository.findByStatus(ElectionStatus.OPEN)));
        assertEquals(List.of("E2"), ids(repository.findByStatus(ElectionStatus.DRAFT)));
        assertEquals(List.of("E3"), ids(repository.findByStatus(ElectionStatus.CLOSED)));
    }

    @Test
    void anAddedElectionCanBeFound() {
        ElectionRepository repository = new ElectionRepository(List.of(TestData.draft("E1")));

        repository.add(TestData.draft("E2"));

        assertEquals(List.of("E1", "E2"), ids(repository.findAll()));
    }

    @Test
    void theSameIdTwiceIsRefusedEvenInAnotherCase() {
        ElectionRepository repository = new ElectionRepository(List.of(TestData.draft("E1")));

        assertThrows(IllegalArgumentException.class, () -> repository.add(TestData.draft("e1")));
        assertThrows(IllegalArgumentException.class,
                () -> new ElectionRepository(List.of(TestData.draft("E1"), TestData.draft("E1"))));
    }

    @Test
    void deleteRemovesAnElectionAndSaysWhetherItExisted() {
        ElectionRepository repository = new ElectionRepository(List.of(TestData.draft("E1"), TestData.draft("E2")));

        assertTrue(repository.delete("e1"));
        assertFalse(repository.delete("E1"));
        assertFalse(repository.delete("E9"));
        assertEquals(List.of("E2"), ids(repository.findAll()));
    }

    @Test
    void nextIdIsOneMoreThanTheHighestNumber() {
        ElectionRepository repository = new ElectionRepository(List.of(TestData.draft("E2"), TestData.draft("E6")));
        assertEquals("E7", repository.nextId());
    }

    @Test
    void nextIdStartsAtE1AndIgnoresIdsThatAreNotENumbers() {
        assertEquals("E1", new ElectionRepository(List.of()).nextId());
        assertEquals("E1", new ElectionRepository(List.of(TestData.draft("SPECIAL"))).nextId());
    }

    @Test
    void nextIdCanReuseTheNumberOfTheHighestDeletedElection() {
        ElectionRepository repository = new ElectionRepository(List.of(TestData.draft("E1"), TestData.draft("E2")));
        repository.delete("E2");
        assertEquals("E2", repository.nextId());
    }

    @Test
    void theListCannotBeChangedFromOutside() {
        ElectionRepository repository = new ElectionRepository(List.of(TestData.draft("E1")));
        assertThrows(UnsupportedOperationException.class, () -> repository.findAll().add(TestData.draft("E2")));
    }
}
