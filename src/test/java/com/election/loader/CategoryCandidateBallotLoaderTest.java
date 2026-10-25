package com.election.loader;

import com.election.TestData;
import com.election.exception.DataLoadException;
import com.election.model.Ballot;
import com.election.model.BallotType;
import com.election.model.Category;
import com.election.model.Election;
import com.election.model.MultipleChoiceBallot;
import com.election.repository.ElectionRepository;
import com.election.service.BallotFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The three loaders that attach what they read to elections that are already loaded. */
class CategoryCandidateBallotLoaderTest {

    @TempDir
    Path folder;

    private Election e1;
    private Election e2;
    private ElectionRepository repository;

    @BeforeEach
    void setUp() {
        e1 = TestData.draft("E1");
        e2 = TestData.draft("E2");
        repository = new ElectionRepository(List.of(e1, e2));
    }

    private static final String CATEGORY_HEADER = "id,electionId,name,ballotType,seats";
    private static final String CANDIDATE_HEADER = "id,name,electionId,categoryId";
    private static final String BALLOT_HEADER = "voterId,electionId,categoryId,choices";

    private String loadCategoriesError(String... rows) throws Exception {
        String[] lines = new String[rows.length + 1];
        lines[0] = CATEGORY_HEADER;
        System.arraycopy(rows, 0, lines, 1, rows.length);
        Path file = CsvFiles.write(folder, "categories.csv", lines);
        return assertThrows(DataLoadException.class, () -> new CategoryCsvLoader(repository).load(file)).getMessage();
    }

    @Nested
    class Categories {

        @Test
        void eachCategoryIsAddedToItsElection() throws Exception {
            Path file = CsvFiles.write(folder, "categories.csv", CATEGORY_HEADER,
                    "CAT1,E1,President,SELECT_ONE,1",
                    "CAT2,E1,Committee,SELECT_MULTIPLE,2",
                    "CAT1,E2,Raise the fee?,YES_NO,1");

            List<Category> loaded = new CategoryCsvLoader(repository).load(file);

            assertEquals(3, loaded.size());
            assertEquals(2, e1.getCategories().size());
            assertEquals(2, e1.findCategory("CAT2").orElseThrow().getSeats());
            assertEquals(BallotType.YES_NO, e2.findCategory("CAT1").orElseThrow().getBallotType());
        }

        @Test
        void theSameCategoryIdInTwoElectionsIsFine() throws Exception {
            Path file = CsvFiles.write(folder, "categories.csv", CATEGORY_HEADER,
                    "CAT1,E1,President,SELECT_ONE,1", "CAT1,E2,President,SELECT_ONE,1");

            new CategoryCsvLoader(repository).load(file);

            assertTrue(e1.findCategory("CAT1").isPresent());
            assertTrue(e2.findCategory("CAT1").isPresent());
        }

        @Test
        void everyBadRowIsRefusedWithItsLineAndReason() throws Exception {
            assertEquals("categories.csv line 2: unknown election E9",
                    loadCategoriesError("CAT1,E9,President,SELECT_ONE,1"));
            assertEquals("categories.csv line 2: 'RANKED' is not one of [SELECT_ONE, SELECT_MULTIPLE, YES_NO]",
                    loadCategoriesError("CAT1,E1,President,RANKED,1"));
            assertEquals("categories.csv line 2: 'two' is not a whole number",
                    loadCategoriesError("CAT1,E1,Committee,SELECT_MULTIPLE,two"));
            assertEquals("categories.csv line 2: Category President is SELECT_ONE, so it has exactly 1 seat",
                    loadCategoriesError("CAT1,E1,President,SELECT_ONE,2"));
        }

        @Test
        void aRepeatedIdInOneElectionIsRefused() throws Exception {
            assertEquals("categories.csv line 3: Duplicate category id CAT1 in E1",
                    loadCategoriesError("CAT1,E1,President,SELECT_ONE,1", "CAT1,E1,Treasurer,SELECT_ONE,1"));
        }

        @Test
        void aRepeatedNameInOneElectionIsRefused() throws Exception {
            assertTrue(loadCategoriesError("CAT1,E1,President,SELECT_ONE,1", "CAT2,E1,president,SELECT_ONE,1")
                    .startsWith("categories.csv line 3: Category president already exists"));
        }
    }

    @Nested
    class Candidates {

        @BeforeEach
        void addCategories() {
            TestData.category(e1, "CAT1", "President", BallotType.SELECT_ONE, 1);
            TestData.category(e1, "CAT2", "Treasurer", BallotType.SELECT_ONE, 1);
            TestData.category(e1, "CAT3", "Raise the fee?", BallotType.YES_NO, 1);
        }

        private String loadError(String row) throws Exception {
            Path file = CsvFiles.write(folder, "candidates.csv", CANDIDATE_HEADER, "C1,Ana,E1,CAT1", row);
            return assertThrows(DataLoadException.class, () -> new CandidateCsvLoader(repository).load(file))
                    .getMessage();
        }

        @Test
        void eachCandidateIsAddedToItsCategory() throws Exception {
            Path file = CsvFiles.write(folder, "candidates.csv", CANDIDATE_HEADER,
                    "C1,Ana,E1,CAT1", "C2,Ben,E1,CAT1", "C3,Cara,E1,CAT2");

            new CandidateCsvLoader(repository).load(file);

            assertEquals(2, e1.findCategory("CAT1").orElseThrow().getCandidates().size());
            assertEquals("Cara", e1.findCategory("CAT2").orElseThrow().getCandidates().get(0).getName());
        }

        // Each test loads only once: a load that fails on its second row has already added its first one.

        @Test
        void anUnknownElectionIsRefusedWithItsLine() throws Exception {
            assertEquals("candidates.csv line 3: unknown election E9", loadError("C2,Ben,E9,CAT1"));
        }

        @Test
        void anUnknownCategoryIsRefusedWithItsLine() throws Exception {
            assertEquals("candidates.csv line 3: unknown category CAT9 in E1", loadError("C2,Ben,E1,CAT9"));
        }

        @Test
        void aCandidateIdIsUniqueAcrossTheWholeElection() throws Exception {
            assertEquals("candidates.csv line 3: duplicate candidate id C1 in E1", loadError("C1,Ben,E1,CAT2"));
        }

        @Test
        void anInvalidNameIsRefused() throws Exception {
            assertTrue(loadError("C2,Zoe#1,E1,CAT1")
                    .startsWith("candidates.csv line 3: Candidate name 'Zoe#1' may only contain"));
        }

        @Test
        void aNameAlreadyStandingInTheCategoryIsRefused() throws Exception {
            assertEquals("candidates.csv line 3: Candidate ana is already standing for President",
                    loadError("C2,ana,E1,CAT1"));
        }

        @Test
        void aYesNoQuestionCannotHaveCandidates() throws Exception {
            assertTrue(loadError("C2,Ben,E1,CAT3").startsWith("candidates.csv line 3: Category Raise the fee? is a yes/no"));
        }
    }

    @Nested
    class Ballots {

        @BeforeEach
        void addCategoriesAndCandidates() throws Exception {
            Category president = TestData.category(e1, "CAT1", "President", BallotType.SELECT_ONE, 1);
            TestData.addCandidate(president, "C1", "Ana");
            TestData.addCandidate(president, "C2", "Ben");
            Category committee = TestData.category(e1, "CAT2", "Committee", BallotType.SELECT_MULTIPLE, 2);
            TestData.addCandidate(committee, "C3", "Cara");
            TestData.addCandidate(committee, "C4", "Dan");
            TestData.addCandidate(committee, "C5", "Eve");
            TestData.category(e1, "CAT3", "Raise the fee?", BallotType.YES_NO, 1);
        }

        private String loadError(String row) throws Exception {
            Path file = CsvFiles.write(folder, "ballots.csv", BALLOT_HEADER, "V001,E1,CAT1,C1", row);
            return assertThrows(DataLoadException.class,
                    () -> new BallotCsvLoader(repository, new BallotFactory()).load(file)).getMessage();
        }

        @Test
        void ballotsAreBuiltByTheFactoryAndStoredInTheirElection() throws Exception {
            Path file = CsvFiles.write(folder, "ballots.csv", BALLOT_HEADER,
                    "V001,E1,CAT1,c1", "V001,E1,CAT2,C3|c5", "V001,E1,CAT3,yes");

            List<Ballot> loaded = new BallotCsvLoader(repository, new BallotFactory()).load(file);

            assertEquals(3, e1.getBallots().size());
            assertEquals(List.of("C1"), loaded.get(0).getChoices());
            assertInstanceOf(MultipleChoiceBallot.class, loaded.get(1));
            assertEquals(List.of("C3", "C5"), loaded.get(1).getChoices());
            assertEquals(List.of("YES"), loaded.get(2).getChoices());
        }

        @Test
        void unknownElectionsAndCategoriesAreRefusedWithTheirLine() throws Exception {
            assertEquals("ballots.csv line 3: unknown election E9", loadError("V002,E9,CAT1,C1"));
            assertEquals("ballots.csv line 3: unknown category VP in E1", loadError("V002,E1,VP,C1"));
        }

        @Test
        void choicesThatDoNotFitTheCategoryAreRefusedLikeAConsoleVote() throws Exception {
            assertEquals("ballots.csv line 3: Choose exactly 1 candidate for President", loadError("V002,E1,CAT1,C1|C2"));
            assertEquals("ballots.csv line 3: C3 is not a candidate for President", loadError("V002,E1,CAT1,C3"));
            assertEquals("ballots.csv line 3: Choose between 1 and 2 candidates for Committee",
                    loadError("V002,E1,CAT2,C3|C4|C5"));
            assertEquals("ballots.csv line 3: Answer YES or NO", loadError("V002,E1,CAT3,MAYBE"));
        }
    }
}
