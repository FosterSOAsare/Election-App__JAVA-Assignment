package com.election.model;

import com.election.exception.InvalidNameException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CategoryTest {

    private static Category president() {
        return new Category("CAT1", "E1", "President", BallotType.SELECT_ONE, 1);
    }

    // --- happy paths

    @Test
    void aNewCategoryKeepsItsDetailsAndStartsWithoutCandidates() {
        Category category = new Category("CAT2", "E1", "Committee", BallotType.SELECT_MULTIPLE, 3);

        assertEquals("CAT2", category.getId());
        assertEquals("E1", category.getElectionId());
        assertEquals("Committee", category.getName());
        assertEquals(BallotType.SELECT_MULTIPLE, category.getBallotType());
        assertEquals(3, category.getSeats());
        assertTrue(category.getCandidates().isEmpty());
    }

    @Test
    void candidatesStayInTheOrderTheyWereAdded() throws InvalidNameException {
        Category president = president();
        president.addCandidate(new Candidate("C2", "Ben", "E1", "CAT1"));
        president.addCandidate(new Candidate("C1", "Ana", "E1", "CAT1"));
        president.addCandidate(new Candidate("C3", "Cara", "E1", "CAT1"));

        assertEquals(List.of("C2", "C1", "C3"), president.getCandidates().stream().map(Candidate::getId).toList());
    }

    @Test
    void theCandidateListCannotBeChangedFromOutside() throws InvalidNameException {
        Category president = president();
        president.addCandidate(new Candidate("C1", "Ana", "E1", "CAT1"));
        Candidate sneaky = new Candidate("C2", "Ben", "E1", "CAT1");

        assertThrows(UnsupportedOperationException.class, () -> president.getCandidates().add(sneaky));
        assertEquals(1, president.getCandidates().size());
    }

    // --- seats per type, with the boundaries

    @Test
    void selectOneAndYesNoHaveExactlyOneSeat() {
        assertDoesNotThrow(() -> new Category("CAT1", "E1", "President", BallotType.SELECT_ONE, 1));
        assertDoesNotThrow(() -> new Category("CAT1", "E1", "Raise the fee?", BallotType.YES_NO, 1));
        for (int seats : new int[]{0, 2}) {
            assertThrows(IllegalArgumentException.class,
                    () -> new Category("CAT1", "E1", "President", BallotType.SELECT_ONE, seats));
            assertThrows(IllegalArgumentException.class,
                    () -> new Category("CAT1", "E1", "Raise the fee?", BallotType.YES_NO, seats));
        }
    }

    @Test
    void selectMultipleNeedsAtLeastTwoSeatsWithNoUpperLimit() {
        assertDoesNotThrow(() -> new Category("CAT1", "E1", "Committee", BallotType.SELECT_MULTIPLE, 2));
        assertDoesNotThrow(() -> new Category("CAT1", "E1", "Committee", BallotType.SELECT_MULTIPLE, 25));
        assertThrows(IllegalArgumentException.class,
                () -> new Category("CAT1", "E1", "Committee", BallotType.SELECT_MULTIPLE, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new Category("CAT1", "E1", "Committee", BallotType.SELECT_MULTIPLE, -1));
    }

    // --- errors

    @Test
    void aMissingIdNameOrTypeIsRefused() {
        assertThrows(IllegalArgumentException.class,
                () -> new Category(" ", "E1", "President", BallotType.SELECT_ONE, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new Category("CAT1", "E1", "", BallotType.SELECT_ONE, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new Category("CAT1", "E1", "President", null, 1));
    }

    @Test
    void aYesNoQuestionRefusesCandidates() throws InvalidNameException {
        Category question = new Category("CAT1", "E1", "Raise the fee?", BallotType.YES_NO, 1);
        Candidate candidate = new Candidate("C1", "Ana", "E1", "CAT1");

        assertThrows(IllegalArgumentException.class, () -> question.addCandidate(candidate));
        assertTrue(question.getCandidates().isEmpty());
    }

    @Test
    void aRepeatedCandidateIdIsRefused() throws InvalidNameException {
        Category president = president();
        president.addCandidate(new Candidate("C1", "Ana", "E1", "CAT1"));

        Candidate sameId = new Candidate("C1", "Ben", "E1", "CAT1");
        assertThrows(IllegalArgumentException.class, () -> president.addCandidate(sameId));
    }

    @Test
    void aNameAlreadyStandingIsRefusedWhateverItsCase() throws InvalidNameException {
        Category president = president();
        president.addCandidate(new Candidate("C1", "Ana Owusu", "E1", "CAT1"));

        Candidate sameName = new Candidate("C2", "ana owusu", "E1", "CAT1");
        assertThrows(IllegalArgumentException.class, () -> president.addCandidate(sameName));
        assertEquals(1, president.getCandidates().size());
    }

    @Test
    void aCandidateOfAnotherCategoryOrElectionIsRefused() throws InvalidNameException {
        Category president = president();
        Candidate forTreasurer = new Candidate("C1", "Ana", "E1", "CAT2");
        Candidate forOtherElection = new Candidate("C1", "Ana", "E2", "CAT1");

        assertThrows(IllegalArgumentException.class, () -> president.addCandidate(forTreasurer));
        assertThrows(IllegalArgumentException.class, () -> president.addCandidate(forOtherElection));
    }

    // --- renaming candidates

    @Test
    void aCandidateCanBeRenamed() throws InvalidNameException {
        Category president = president();
        Candidate ana = new Candidate("C1", "Ana", "E1", "CAT1");
        president.addCandidate(ana);

        president.renameCandidate(ana, "Ana Owusu");

        assertEquals("Ana Owusu", ana.getName());
        assertEquals("C1", ana.getId());
    }

    @Test
    void aCandidateMayBeRenamedToADifferentCaseOfItsOwnName() throws InvalidNameException {
        Category president = president();
        Candidate ana = new Candidate("C1", "ana", "E1", "CAT1");
        president.addCandidate(ana);

        president.renameCandidate(ana, "Ana");

        assertEquals("Ana", ana.getName());
    }

    @Test
    void renamingToAnotherCandidatesNameIsRefused() throws InvalidNameException {
        Category president = president();
        Candidate ana = new Candidate("C1", "Ana", "E1", "CAT1");
        president.addCandidate(ana);
        president.addCandidate(new Candidate("C2", "Ben", "E1", "CAT1"));

        assertThrows(InvalidNameException.class, () -> president.renameCandidate(ana, "ben"));
        assertEquals("Ana", ana.getName());
    }

    @Test
    void renamingStillAppliesTheNameRules() throws InvalidNameException {
        Category president = president();
        Candidate ana = new Candidate("C1", "Ana", "E1", "CAT1");
        president.addCandidate(ana);

        assertThrows(InvalidNameException.class, () -> president.renameCandidate(ana, "Ana#1"));
        assertEquals("Ana", ana.getName());
    }

    @Test
    void aCandidateOfAnotherCategoryCannotBeRenamedHere() throws InvalidNameException {
        Category president = president();
        Candidate stranger = new Candidate("C9", "Zed", "E1", "CAT1");

        assertThrows(IllegalArgumentException.class, () -> president.renameCandidate(stranger, "Zed Two"));
    }
}
