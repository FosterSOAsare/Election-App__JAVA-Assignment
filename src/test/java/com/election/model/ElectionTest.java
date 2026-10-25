package com.election.model;

import com.election.TestData;
import com.election.exception.ElectionStateException;
import com.election.exception.InvalidNameException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ElectionTest {

    @Test
    void aNewElectionKeepsItsDetailsAndStartsEmpty() {
        LocalDateTime deadline = Deadlines.parse("2030-01-01 18:00");
        Election election = new Election("E7", "Chess Club 2030", deadline, ElectionStatus.DRAFT, "A1");

        assertEquals("E7", election.getId());
        assertEquals("Chess Club 2030", election.getName());
        assertEquals(deadline, election.getDeadline());
        assertEquals(ElectionStatus.DRAFT, election.getStatus());
        assertEquals("A1", election.getCreatedBy());
        assertTrue(election.getCategories().isEmpty());
        assertTrue(election.getBallots().isEmpty());
    }

    @Test
    void aMissingIdOrNameIsRefused() {
        assertThrows(IllegalArgumentException.class,
                () -> new Election(" ", "Name", TestData.FUTURE, ElectionStatus.DRAFT, "A1"));
        assertThrows(IllegalArgumentException.class,
                () -> new Election("E1", "", TestData.FUTURE, ElectionStatus.DRAFT, "A1"));
    }

    /** DRAFT -> OPEN -> CLOSED, never backwards and never skipping a step. */
    @Nested
    class Lifecycle {

        @Test
        void aDraftCanBeOpenedAndThenClosed() throws ElectionStateException {
            Election election = TestData.draft("E1");

            election.open();
            assertEquals(ElectionStatus.OPEN, election.getStatus());

            election.close();
            assertEquals(ElectionStatus.CLOSED, election.getStatus());
        }

        @Test
        void onlyADraftCanBeOpened() {
            assertThrows(ElectionStateException.class, () -> TestData.election("E1", ElectionStatus.OPEN).open());
            assertThrows(ElectionStateException.class, () -> TestData.election("E1", ElectionStatus.CLOSED).open());
        }

        @Test
        void aDraftCannotSkipStraightToClosed() {
            Election election = TestData.draft("E1");
            assertThrows(ElectionStateException.class, election::close);
            assertEquals(ElectionStatus.DRAFT, election.getStatus());
        }

        @Test
        void aClosedElectionCanNeverChangeAgain() throws ElectionStateException {
            Election election = TestData.draft("E1");
            election.open();
            election.close();

            assertThrows(ElectionStateException.class, election::open);
            assertThrows(ElectionStateException.class, election::close);
            assertEquals(ElectionStatus.CLOSED, election.getStatus());
        }

        @Test
        void theRefusalSaysWhatTheStatusIs() {
            ElectionStateException e = assertThrows(ElectionStateException.class,
                    () -> TestData.election("E1", ElectionStatus.CLOSED).open());
            assertTrue(e.getMessage().contains("CLOSED"));
        }
    }

    @Nested
    class Details {

        @Test
        void aDraftCanBeRenamedAndGivenANewDeadline() throws Exception {
            Election election = TestData.draft("E1");
            LocalDateTime later = Deadlines.parse("2031-06-01 18:00");

            election.rename("Chess Club 2031");
            election.changeDeadline(later);

            assertEquals("Chess Club 2031", election.getName());
            assertEquals(later, election.getDeadline());
        }

        @Test
        void aNameMayUseQuestionMarksAndColonsButNotCommas() throws Exception {
            Election election = TestData.draft("E1");

            election.rename("Vote 2031: fees?");
            assertEquals("Vote 2031: fees?", election.getName());

            assertThrows(InvalidNameException.class, () -> election.rename("Debate, Club"));
            assertEquals("Vote 2031: fees?", election.getName());
        }

        @Test
        void aNewNameCannotBeEmpty() {
            Election election = TestData.draft("E1");
            assertThrows(InvalidNameException.class, () -> election.rename(" "));
            assertThrows(InvalidNameException.class, () -> election.rename(null));
            assertEquals("Election E1", election.getName());
        }

        @Test
        void aDeadlineIsRequired() {
            Election election = TestData.draft("E1");
            assertThrows(IllegalArgumentException.class, () -> election.changeDeadline(null));
            assertEquals(TestData.FUTURE, election.getDeadline());
        }

        @Test
        void anOpenOrClosedElectionCannotBeRenamedOrGivenANewDeadline() {
            for (ElectionStatus status : List.of(ElectionStatus.OPEN, ElectionStatus.CLOSED)) {
                Election election = TestData.election("E1", status);
                assertThrows(ElectionStateException.class, () -> election.rename("New name"));
                assertThrows(ElectionStateException.class, () -> election.changeDeadline(TestData.FUTURE));
                assertEquals("Election E1", election.getName());
            }
        }
    }

    @Nested
    class Categories {

        @Test
        void categoriesStayInTheOrderTheyWereAddedAndCanBeFoundById() {
            Election election = TestData.draft("E1");
            Category treasurer = TestData.category(election, "CAT2", "Treasurer", BallotType.SELECT_ONE, 1);
            Category president = TestData.category(election, "CAT1", "President", BallotType.SELECT_ONE, 1);

            assertEquals(List.of(treasurer, president), election.getCategories());
            assertSame(president, election.findCategory("CAT1").orElseThrow());
        }

        @Test
        void anUnknownCategoryIdFindsNothing() {
            Election election = TestData.draft("E1");
            TestData.category(election, "CAT1", "President", BallotType.SELECT_ONE, 1);

            assertTrue(election.findCategory("CAT9").isEmpty());
        }

        @Test
        void theCategoryListCannotBeChangedFromOutside() {
            Election election = TestData.draft("E1");
            Category sneaky = new Category("CAT1", "E1", "President", BallotType.SELECT_ONE, 1);

            assertThrows(UnsupportedOperationException.class, () -> election.getCategories().add(sneaky));
            assertTrue(election.getCategories().isEmpty());
        }

        @Test
        void aCategoryWithTheSameIdOrNameIsRefused() {
            Election election = TestData.draft("E1");
            TestData.category(election, "CAT1", "President", BallotType.SELECT_ONE, 1);

            Category sameId = new Category("CAT1", "E1", "Treasurer", BallotType.SELECT_ONE, 1);
            Category sameName = new Category("CAT2", "E1", "president", BallotType.SELECT_ONE, 1);
            assertThrows(IllegalArgumentException.class, () -> election.addCategory(sameId));
            assertThrows(IllegalArgumentException.class, () -> election.addCategory(sameName));
            assertEquals(1, election.getCategories().size());
        }

        @Test
        void aCategoryOfAnotherElectionIsRefused() {
            Election election = TestData.draft("E1");
            Category ofE2 = new Category("CAT1", "E2", "President", BallotType.SELECT_ONE, 1);
            assertThrows(IllegalArgumentException.class, () -> election.addCategory(ofE2));
        }

        @Test
        void aCategoryCanBeRenamedIncludingToAnotherCaseOfItsOwnName() throws InvalidNameException {
            Election election = TestData.draft("E1");
            Category president = TestData.category(election, "CAT1", "president", BallotType.SELECT_ONE, 1);

            election.renameCategory(president, "President");
            assertEquals("President", president.getName());

            election.renameCategory(president, "Chair");
            assertEquals("Chair", president.getName());
        }

        @Test
        void renamingACategoryRefusesEmptyCommaAndAnotherCategorysName() {
            Election election = TestData.draft("E1");
            Category president = TestData.category(election, "CAT1", "President", BallotType.SELECT_ONE, 1);
            TestData.category(election, "CAT2", "Treasurer", BallotType.SELECT_ONE, 1);

            assertThrows(InvalidNameException.class, () -> election.renameCategory(president, " "));
            assertThrows(InvalidNameException.class, () -> election.renameCategory(president, "Vice, president"));
            assertThrows(InvalidNameException.class, () -> election.renameCategory(president, "treasurer"));
            assertEquals("President", president.getName());
        }

        @Test
        void aCategoryOfAnotherElectionCannotBeRenamedHere() {
            Election election = TestData.draft("E1");
            Category stranger = new Category("CAT1", "E2", "President", BallotType.SELECT_ONE, 1);
            assertThrows(IllegalArgumentException.class, () -> election.renameCategory(stranger, "Chair"));
        }

        @Test
        void changingACategoryToYesNoRemovesItsCandidates() throws Exception {
            Election election = TestData.draftWithPresident("E1");
            Category president = election.findCategory("CAT1").orElseThrow();

            election.changeCategoryType(president, BallotType.YES_NO, 1);

            assertEquals(BallotType.YES_NO, president.getBallotType());
            assertTrue(president.getCandidates().isEmpty());
        }

        @Test
        void changingSelectOneToSelectMultipleKeepsTheCandidates() throws Exception {
            Election election = TestData.draftWithPresident("E1");
            Category president = election.findCategory("CAT1").orElseThrow();

            election.changeCategoryType(president, BallotType.SELECT_MULTIPLE, 2);

            assertEquals(BallotType.SELECT_MULTIPLE, president.getBallotType());
            assertEquals(2, president.getSeats());
            assertEquals(2, president.getCandidates().size());
        }

        @Test
        void aNewTypeStillFollowsTheSeatRules() throws Exception {
            Election election = TestData.draftWithPresident("E1");
            Category president = election.findCategory("CAT1").orElseThrow();

            assertThrows(IllegalArgumentException.class,
                    () -> election.changeCategoryType(president, BallotType.SELECT_MULTIPLE, 1));
            assertThrows(IllegalArgumentException.class,
                    () -> election.changeCategoryType(president, BallotType.YES_NO, 2));
            assertEquals(BallotType.SELECT_ONE, president.getBallotType());
            assertEquals(2, president.getCandidates().size());
        }

        @Test
        void onlyADraftsCategoriesCanChangeType() throws Exception {
            Election election = TestData.draftWithPresident("E1");
            Category president = election.findCategory("CAT1").orElseThrow();
            election.open();

            assertThrows(ElectionStateException.class,
                    () -> election.changeCategoryType(president, BallotType.YES_NO, 1));
            assertEquals(BallotType.SELECT_ONE, president.getBallotType());
        }
    }

    @Nested
    class Removing {

        @Test
        void removingNeverRenumbersTheOthers() throws Exception {
            Election election = TestData.draftWithPresident("E1");
            TestData.category(election, "CAT2", "Treasurer", BallotType.SELECT_ONE, 1);
            Category president = election.findCategory("CAT1").orElseThrow();

            election.removeCandidate(president, president.getCandidates().get(0));
            election.removeCategory(election.findCategory("CAT2").orElseThrow());

            assertEquals(List.of("C2"), president.getCandidates().stream().map(Candidate::getId).toList());
            assertEquals(List.of("CAT1"), election.getCategories().stream().map(Category::getId).toList());
        }

        @Test
        void removingACategoryAlsoRemovesItsCandidates() throws Exception {
            Election election = TestData.draftWithPresident("E1");

            election.removeCategory(election.findCategory("CAT1").orElseThrow());

            assertTrue(election.getCategories().isEmpty());
            assertTrue(election.findCategory("CAT1").isEmpty());
        }

        @Test
        void somethingOfAnotherElectionCannotBeRemovedHere() throws Exception {
            Election election = TestData.draftWithPresident("E1");
            Election other = TestData.draftWithPresident("E2");
            Category otherPresident = other.findCategory("CAT1").orElseThrow();

            assertThrows(IllegalArgumentException.class, () -> election.removeCategory(otherPresident));
            assertThrows(IllegalArgumentException.class,
                    () -> election.removeCandidate(otherPresident, otherPresident.getCandidates().get(0)));
        }

        @Test
        void onlyADraftCanLoseCategoriesOrCandidates() throws Exception {
            Election election = TestData.draftWithPresident("E1");
            Category president = election.findCategory("CAT1").orElseThrow();
            Candidate ana = president.getCandidates().get(0);
            election.open();

            assertThrows(ElectionStateException.class, () -> election.removeCandidate(president, ana));
            assertThrows(ElectionStateException.class, () -> election.removeCategory(president));
            assertEquals(2, president.getCandidates().size());
        }
    }

    @Nested
    class Ballots {

        @Test
        void ballotsAreKeptInTheOrderTheyArrive() throws Exception {
            Election election = TestData.draftWithPresident("E1");
            Ballot first = new SingleChoiceBallot("V001", "E1", "CAT1", List.of("C1"));
            Ballot second = new SingleChoiceBallot("V002", "E1", "CAT1", List.of("C2"));

            election.addBallot(first);
            election.addBallot(second);

            assertEquals(List.of(first, second), election.getBallots());
        }

        @Test
        void aBallotOfAnotherElectionIsRefused() {
            Election election = TestData.draft("E1");
            Ballot forE2 = new SingleChoiceBallot("V001", "E2", "CAT1", List.of("C1"));

            assertThrows(IllegalArgumentException.class, () -> election.addBallot(forE2));
            assertTrue(election.getBallots().isEmpty());
        }

        @Test
        void theBallotListCannotBeChangedFromOutside() {
            Election election = TestData.draft("E1");
            Ballot sneaky = new SingleChoiceBallot("V001", "E1", "CAT1", List.of("C1"));

            assertThrows(UnsupportedOperationException.class, () -> election.getBallots().add(sneaky));
        }
    }
}
