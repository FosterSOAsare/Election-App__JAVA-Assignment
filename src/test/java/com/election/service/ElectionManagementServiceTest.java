package com.election.service;

import com.election.TestData;
import com.election.exception.ElectionException;
import com.election.exception.ElectionStateException;
import com.election.exception.InvalidNameException;
import com.election.exception.UnauthorizedActionException;
import com.election.model.Administrator;
import com.election.model.BallotType;
import com.election.model.Candidate;
import com.election.model.Category;
import com.election.model.Deadlines;
import com.election.model.Election;
import com.election.model.ElectionStatus;
import com.election.model.User;
import com.election.repository.ElectionRepository;
import com.election.service.ElectionManagementService.NewCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ElectionManagementServiceTest {

    /** "Today" in every test: 2026-11-01 12:00. */
    private static final String NOW = "2026-11-01T12:00:00";

    private final User a1 = TestData.admin();
    private final User a2 = new Administrator("A2");
    private ElectionRepository repository;
    private ElectionManagementService service;

    @BeforeEach
    void setUp() {
        repository = new ElectionRepository(List.of());
        service = new ElectionManagementService(repository, TestData.clockAt(NOW));
    }

    private Election store(Election election) {
        repository.add(election);
        return election;
    }

    private static List<String> ids(List<?> items) {
        return items.stream().map(item -> item instanceof Category c ? c.getId() : ((Candidate) item).getId()).toList();
    }

    @Nested
    class Creating {

        @Test
        void aNewElectionIsADraftOwnedByItsCreatorWithTheNextId() throws ElectionException {
            store(TestData.draft("E6"));

            Election created = service.createElection(a1, "Chess Club", TestData.FUTURE, List.of(
                    new NewCategory("President", BallotType.SELECT_ONE, 1, List.of("Ana", "Ben")),
                    new NewCategory("Committee", BallotType.SELECT_MULTIPLE, 2, List.of("Cara", "Dan", "Eve")),
                    new NewCategory("Raise the fee?", BallotType.YES_NO, 1, List.of())));

            assertEquals("E7", created.getId());
            assertEquals(ElectionStatus.DRAFT, created.getStatus());
            assertEquals("A1", created.getCreatedBy());
            assertSame(created, repository.findById("E7").orElseThrow());
        }

        @Test
        void categoriesAreNumberedInsideTheElectionAndCandidatesAcrossIt() throws ElectionException {
            Election created = service.createElection(a1, "Chess Club", TestData.FUTURE, List.of(
                    new NewCategory("President", BallotType.SELECT_ONE, 1, List.of("Ana", "Ben")),
                    new NewCategory("Committee", BallotType.SELECT_MULTIPLE, 2, List.of("Cara", "Dan", "Eve"))));

            assertEquals(List.of("CAT1", "CAT2"), ids(created.getCategories()));
            assertEquals(List.of("C1", "C2"), ids(created.getCategories().get(0).getCandidates()));
            assertEquals(List.of("C3", "C4", "C5"), ids(created.getCategories().get(1).getCandidates()));
        }

        @Test
        void aNameWithACommaOrNoNameIsRefusedAndNothingIsStored() {
            assertThrows(InvalidNameException.class,
                    () -> service.createElection(a1, "Debate, Club", TestData.FUTURE, List.of()));
            assertThrows(InvalidNameException.class,
                    () -> service.createElection(a1, " ", TestData.FUTURE, List.of()));
            assertTrue(repository.findAll().isEmpty());
        }

        @Test
        void aDeadlineThatHasPassedOrIsRightNowIsRefused() {
            LocalDateTime yesterday = Deadlines.parse("2026-10-31");
            LocalDateTime rightNow = Deadlines.parse("2026-11-01 12:00");

            assertThrows(ElectionStateException.class,
                    () -> service.createElection(a1, "Chess Club", yesterday, List.of()));
            assertThrows(ElectionStateException.class,
                    () -> service.createElection(a1, "Chess Club", rightNow, List.of()));
            assertTrue(repository.findAll().isEmpty());
        }

        @Test
        void oneInvalidCandidateNameStoresNothingAtAll() {
            assertThrows(InvalidNameException.class, () -> service.createElection(a1, "Chess Club", TestData.FUTURE,
                    List.of(new NewCategory("President", BallotType.SELECT_ONE, 1, List.of("Ana", "B@n")))));
            assertTrue(repository.findAll().isEmpty());
        }
    }

    @Nested
    class Finding {

        @Test
        void anAdminOnlySeesTheirOwnElectionsInTheStatusAskedFor() {
            store(TestData.draft("E1"));
            store(new Election("E2", "A2's draft", TestData.FUTURE, ElectionStatus.DRAFT, "A2"));
            store(TestData.election("E3", ElectionStatus.OPEN));

            assertEquals(List.of("E1"), service.findAdminElections(a1, ElectionStatus.DRAFT).stream()
                    .map(Election::getId).toList());
            assertEquals(List.of("E3"), service.findAdminElections(a1, ElectionStatus.OPEN).stream()
                    .map(Election::getId).toList());
            assertEquals(List.of("E2"), service.findAdminElections(a2, ElectionStatus.DRAFT).stream()
                    .map(Election::getId).toList());
            assertTrue(service.findAdminElections(a2, ElectionStatus.OPEN).isEmpty());
        }

        @Test
        void allElectionsListsEveryone() {
            store(TestData.draft("E1"));
            store(new Election("E2", "A2's draft", TestData.FUTURE, ElectionStatus.DRAFT, "A2"));

            assertEquals(2, service.allElections().size());
        }
    }

    @Nested
    class Opening {

        @Test
        void aReadyDraftIsOpened() throws Exception {
            Election draft = store(TestData.draftWithPresident("E1"));

            service.openElection(a1, draft);

            assertEquals(ElectionStatus.OPEN, draft.getStatus());
        }

        @Test
        void aDraftWithOnlyYesNoQuestionsNeedsNoCandidates() throws Exception {
            Election draft = store(TestData.draft("E1"));
            TestData.category(draft, "CAT1", "Raise the fee?", BallotType.YES_NO, 1);

            service.openElection(a1, draft);

            assertEquals(ElectionStatus.OPEN, draft.getStatus());
        }

        @Test
        void aDraftWithoutCategoriesCannotOpen() {
            Election draft = store(TestData.draft("E1"));

            ElectionStateException e = assertThrows(ElectionStateException.class,
                    () -> service.openElection(a1, draft));
            assertEquals("E1 is not ready to open: it has no categories", e.getMessage());
            assertEquals(ElectionStatus.DRAFT, draft.getStatus());
        }

        @Test
        void everySetupProblemIsReportedAtOnce() throws Exception {
            Election draft = store(new Election("E1", "Not ready", Deadlines.parse("2026-10-01"),
                    ElectionStatus.DRAFT, "A1"));
            TestData.category(draft, "CAT1", "President", BallotType.SELECT_ONE, 1);
            Category board = TestData.category(draft, "CAT2", "Board", BallotType.SELECT_MULTIPLE, 2);
            TestData.addCandidate(board, "C1", "Ana");
            TestData.addCandidate(board, "C2", "Ben");

            ElectionStateException e = assertThrows(ElectionStateException.class,
                    () -> service.openElection(a1, draft));

            assertTrue(e.getMessage().contains("deadline 2026-10-01 00:00:00 has already passed"));
            assertTrue(e.getMessage().contains("President has no candidates"));
            assertTrue(e.getMessage().contains("Board has 2 candidates for 2 seats"));
        }

        @Test
        void exactlyOneMoreCandidateThanSeatsIsEnough() throws Exception {
            Election draft = store(TestData.draft("E1"));
            Category board = TestData.category(draft, "CAT1", "Board", BallotType.SELECT_MULTIPLE, 2);
            TestData.addCandidate(board, "C1", "Ana");
            TestData.addCandidate(board, "C2", "Ben");
            TestData.addCandidate(board, "C3", "Cara");

            service.openElection(a1, draft);

            assertEquals(ElectionStatus.OPEN, draft.getStatus());
        }

        @Test
        void onlyADraftCanBeOpened() {
            Election open = store(TestData.election("E1", ElectionStatus.OPEN));
            assertThrows(ElectionStateException.class, () -> service.openElection(a1, open));
        }
    }

    @Nested
    class Closing {

        @Test
        void theCreatorClosesAnOpenElection() throws Exception {
            Election open = store(TestData.election("E1", ElectionStatus.OPEN));

            service.closeElection(a1, open);

            assertEquals(ElectionStatus.CLOSED, open.getStatus());
        }

        @Test
        void onlyAnOpenElectionCanBeClosed() {
            Election draft = store(TestData.draft("E1"));
            assertThrows(ElectionStateException.class, () -> service.closeElection(a1, draft));
            assertEquals(ElectionStatus.DRAFT, draft.getStatus());
        }

        @Test
        void closingBeforeTheDeadlineIsAllowedForTesting() throws Exception {
            Election open = store(TestData.election("E1", ElectionStatus.OPEN));
            assertFalse(service.deadlinePassed(open));

            service.closeElection(a1, open);

            assertEquals(ElectionStatus.CLOSED, open.getStatus());
        }
    }

    @Nested
    class Permissions {

        private interface AdminAction {
            void run(User user, Election election) throws Exception;
        }

        /** Every action that changes an election, run on E1's President category and its first candidate. */
        private Map<String, AdminAction> actions() {
            return Map.ofEntries(
                    Map.entry("open", (user, e) -> service.openElection(user, e)),
                    Map.entry("delete", (user, e) -> service.deleteElection(user, e)),
                    Map.entry("details", (user, e) -> service.updateElectionDetails(user, e, "New", null)),
                    Map.entry("add categories", (user, e) -> service.addCategories(user, e,
                            List.of(new NewCategory("Secretary", BallotType.YES_NO, 1, List.of())))),
                    Map.entry("rename category", (user, e) -> service.renameCategory(user, e, president(e), "Chair")),
                    Map.entry("change type", (user, e) -> service.changeCategoryType(user, e, president(e),
                            BallotType.YES_NO, 1, List.of())),
                    Map.entry("remove category", (user, e) -> service.removeCategory(user, e, president(e))),
                    Map.entry("add candidates", (user, e) -> service.addCandidates(user, e, president(e),
                            List.of("Cara"))),
                    Map.entry("rename candidate", (user, e) -> service.renameCandidate(user, e, president(e),
                            ana(e), "Anna")),
                    Map.entry("remove candidate", (user, e) -> service.removeCandidate(user, e, president(e),
                            ana(e))));
        }

        private Category president(Election election) {
            return election.findCategory("CAT1").orElseThrow();
        }

        private Candidate ana(Election election) {
            return president(election).getCandidates().get(0);
        }

        @Test
        void aVoterCanDoNoneOfThem() throws Exception {
            for (Map.Entry<String, AdminAction> action : actions().entrySet()) {
                Election draft = TestData.draftWithPresident("E1");
                assertThrows(UnauthorizedActionException.class,
                        () -> action.getValue().run(TestData.voter(), draft), action.getKey());
            }
        }

        @Test
        void anotherAdminCanDoNoneOfThem() throws Exception {
            for (Map.Entry<String, AdminAction> action : actions().entrySet()) {
                Election draft = TestData.draftWithPresident("E1");
                assertThrows(UnauthorizedActionException.class, () -> action.getValue().run(a2, draft),
                        action.getKey());
            }
        }

        @Test
        void noneOfThemWorksOnceAnElectionIsOpen() throws Exception {
            for (Map.Entry<String, AdminAction> action : actions().entrySet()) {
                Election open = TestData.draftWithPresident("E1");
                open.open();
                assertThrows(ElectionStateException.class, () -> action.getValue().run(a1, open), action.getKey());
            }
        }

        @Test
        void anotherAdminCannotCloseEither() {
            Election open = store(TestData.election("E1", ElectionStatus.OPEN));
            assertThrows(UnauthorizedActionException.class, () -> service.closeElection(a2, open));
            assertThrows(UnauthorizedActionException.class, () -> service.closeElection(TestData.voter(), open));
            assertEquals(ElectionStatus.OPEN, open.getStatus());
        }
    }

    @Nested
    class EditingDetails {

        @Test
        void nameAndDeadlineCanBeChangedTogetherOrOneAtATime() throws Exception {
            Election draft = store(TestData.draft("E1"));
            LocalDateTime later = Deadlines.parse("2031-01-01 18:00");

            service.updateElectionDetails(a1, draft, "Chess Club", null);
            assertEquals("Chess Club", draft.getName());
            assertEquals(TestData.FUTURE, draft.getDeadline());

            service.updateElectionDetails(a1, draft, null, later);
            assertEquals("Chess Club", draft.getName());
            assertEquals(later, draft.getDeadline());
        }

        @Test
        void aBadNameChangesNothingNotEvenTheDeadline() {
            Election draft = store(TestData.draft("E1"));

            assertThrows(InvalidNameException.class, () -> service.updateElectionDetails(a1, draft, "Chess, Club",
                    Deadlines.parse("2031-01-01")));
            assertEquals("Election E1", draft.getName());
            assertEquals(TestData.FUTURE, draft.getDeadline());
        }

        @Test
        void aDeadlineThatHasPassedChangesNothingNotEvenTheName() {
            Election draft = store(TestData.draft("E1"));

            assertThrows(ElectionStateException.class, () -> service.updateElectionDetails(a1, draft, "Chess Club",
                    Deadlines.parse("2026-10-01")));
            assertEquals("Election E1", draft.getName());
            assertEquals(TestData.FUTURE, draft.getDeadline());
        }
    }

    @Nested
    class EditingCategories {

        @Test
        void addedCategoriesAndCandidatesContinueTheNumbering() throws Exception {
            Election draft = store(TestData.draftWithPresident("E1"));   // CAT1 with C1, C2

            service.addCategories(a1, draft, List.of(
                    new NewCategory("Treasurer", BallotType.SELECT_ONE, 1, List.of("Cara", "Dan")),
                    new NewCategory("Raise the fee?", BallotType.YES_NO, 1, List.of())));

            assertEquals(List.of("CAT1", "CAT2", "CAT3"), ids(draft.getCategories()));
            assertEquals(List.of("C3", "C4"), ids(draft.getCategories().get(1).getCandidates()));
        }

        @Test
        void aCategoryNameAlreadyUsedAddsNothingAtAll() throws Exception {
            Election draft = store(TestData.draftWithPresident("E1"));

            assertThrows(InvalidNameException.class, () -> service.addCategories(a1, draft, List.of(
                    new NewCategory("Treasurer", BallotType.SELECT_ONE, 1, List.of("Cara")),
                    new NewCategory("president", BallotType.SELECT_ONE, 1, List.of("Dan")))));
            assertEquals(List.of("CAT1"), ids(draft.getCategories()));
        }

        @Test
        void aCategoryCanBeRenamedOrRemoved() throws Exception {
            Election draft = store(TestData.draftWithPresident("E1"));
            Category president = draft.findCategory("CAT1").orElseThrow();

            service.renameCategory(a1, draft, president, "Chair");
            assertEquals("Chair", president.getName());

            service.removeCategory(a1, draft, president);
            assertTrue(draft.getCategories().isEmpty());
        }

        @Test
        void yesNoToSelectOneAddsTheNewCandidatesWithTheNextIds() throws Exception {
            Election draft = store(TestData.draftWithPresident("E1"));   // C1, C2 already used
            Category question = TestData.category(draft, "CAT2", "Raise the fee?", BallotType.YES_NO, 1);

            service.changeCategoryType(a1, draft, question, BallotType.SELECT_ONE, 1, List.of("Cara", "Dan"));

            assertEquals(BallotType.SELECT_ONE, question.getBallotType());
            assertEquals(List.of("C3", "C4"), ids(question.getCandidates()));
        }

        @Test
        void selectOneToYesNoRemovesTheCandidates() throws Exception {
            Election draft = store(TestData.draftWithPresident("E1"));
            Category president = draft.findCategory("CAT1").orElseThrow();

            service.changeCategoryType(a1, draft, president, BallotType.YES_NO, 1, List.of());

            assertTrue(president.getCandidates().isEmpty());
        }

        @Test
        void selectOneToSelectMultipleChangesTheSeats() throws Exception {
            Election draft = store(TestData.draftWithPresident("E1"));
            Category president = draft.findCategory("CAT1").orElseThrow();

            service.changeCategoryType(a1, draft, president, BallotType.SELECT_MULTIPLE, 2, List.of("Cara"));

            assertEquals(2, president.getSeats());
            assertEquals(List.of("C1", "C2", "C3"), ids(president.getCandidates()));
        }

        @Test
        void aNewCandidateAlreadyStandingChangesNothing() throws Exception {
            Election draft = store(TestData.draftWithPresident("E1"));
            Category president = draft.findCategory("CAT1").orElseThrow();

            assertThrows(InvalidNameException.class, () -> service.changeCategoryType(a1, draft, president,
                    BallotType.SELECT_MULTIPLE, 2, List.of("ana")));
            assertEquals(BallotType.SELECT_ONE, president.getBallotType());
            assertEquals(2, president.getCandidates().size());
        }

        @Test
        void aYesNoQuestionCannotBeGivenCandidates() throws Exception {
            Election draft = store(TestData.draftWithPresident("E1"));
            Category president = draft.findCategory("CAT1").orElseThrow();

            assertThrows(IllegalArgumentException.class, () -> service.changeCategoryType(a1, draft, president,
                    BallotType.YES_NO, 1, List.of("Cara")));
        }
    }

    @Nested
    class EditingCandidates {

        @Test
        void addedCandidatesGetTheNextIdsAcrossTheElection() throws Exception {
            Election draft = store(TestData.draftWithPresident("E1"));
            Category treasurer = TestData.category(draft, "CAT2", "Treasurer", BallotType.SELECT_ONE, 1);
            TestData.addCandidate(treasurer, "C3", "Cara");
            Category president = draft.findCategory("CAT1").orElseThrow();

            service.addCandidates(a1, draft, president, List.of("Dan", "Eve"));

            assertEquals(List.of("C1", "C2", "C4", "C5"), ids(president.getCandidates()));
        }

        @Test
        void aNameAlreadyStandingOrTypedTwiceAddsNothing() throws Exception {
            Election draft = store(TestData.draftWithPresident("E1"));
            Category president = draft.findCategory("CAT1").orElseThrow();

            assertThrows(InvalidNameException.class,
                    () -> service.addCandidates(a1, draft, president, List.of("Cara", "ben")));
            assertThrows(InvalidNameException.class,
                    () -> service.addCandidates(a1, draft, president, List.of("Cara", "cara")));
            assertEquals(2, president.getCandidates().size());
        }

        @Test
        void aYesNoQuestionCannotGetCandidates() throws Exception {
            Election draft = store(TestData.draftWithPresident("E1"));
            Category question = TestData.category(draft, "CAT2", "Raise the fee?", BallotType.YES_NO, 1);

            assertThrows(ElectionStateException.class,
                    () -> service.addCandidates(a1, draft, question, List.of("Cara")));
        }

        @Test
        void aCandidateCanBeRenamedOrRemoved() throws Exception {
            Election draft = store(TestData.draftWithPresident("E1"));
            Category president = draft.findCategory("CAT1").orElseThrow();
            Candidate ana = president.getCandidates().get(0);

            service.renameCandidate(a1, draft, president, ana, "Ana Owusu");
            assertEquals("Ana Owusu", ana.getName());

            service.removeCandidate(a1, draft, president, ana);
            assertEquals(List.of("C2"), ids(president.getCandidates()));
        }
    }

    @Nested
    class Deleting {

        @Test
        void aDraftIsDeletedFromTheRepository() throws Exception {
            Election draft = store(TestData.draftWithPresident("E1"));

            service.deleteElection(a1, draft);

            assertTrue(repository.findById("E1").isEmpty());
        }

        @Test
        void anOpenOrClosedElectionIsNeverDeletedBecauseItMayHaveBallots() {
            Election open = store(TestData.election("E1", ElectionStatus.OPEN));
            Election closed = store(TestData.election("E2", ElectionStatus.CLOSED));

            assertThrows(ElectionStateException.class, () -> service.deleteElection(a1, open));
            assertThrows(ElectionStateException.class, () -> service.deleteElection(a1, closed));
            assertEquals(2, repository.findAll().size());
        }
    }

    @Test
    void theDeadlinePassesAtExactlyItsMoment() {
        assertFalse(service.deadlinePassed(Deadlines.parse("2026-11-01 12:00:01")));
        assertTrue(service.deadlinePassed(Deadlines.parse("2026-11-01 12:00")));
        assertTrue(service.deadlinePassed(Deadlines.parse("2026-11-01 11:59:59")));
    }
}
