package com.election.model;

import com.election.TestData;
import com.election.exception.InvalidBallotException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Each ballot type checks its own choices and stores them exactly as the category spells them. */
class BallotTest {

    private Category president;   // Select one: C1 Ana, C2 Ben
    private Category committee;   // Select multiple, 2 seats: C3 Cara, C4 Dan, C5 Eve
    private Category feeRise;     // Yes / No

    @BeforeEach
    void setUp() throws Exception {
        Election election = TestData.draftWithPresident("E1");
        president = election.findCategory("CAT1").orElseThrow();
        committee = TestData.category(election, "CAT2", "Committee", BallotType.SELECT_MULTIPLE, 2);
        TestData.addCandidate(committee, "C3", "Cara");
        TestData.addCandidate(committee, "C4", "Dan");
        TestData.addCandidate(committee, "C5", "Eve");
        feeRise = TestData.category(election, "CAT3", "Raise the fee?", BallotType.YES_NO, 1);
    }

    @Test
    void aBallotKeepsWhoVotedWhereAndWhat() {
        Ballot ballot = new SingleChoiceBallot("V001", "E1", "CAT1", List.of("C1"));

        assertEquals("V001", ballot.getVoterId());
        assertEquals("E1", ballot.getElectionId());
        assertEquals("CAT1", ballot.getCategoryId());
        assertEquals(List.of("C1"), ballot.getChoices());
    }

    @Nested
    class SelectOne {

        @Test
        void oneCandidateOfTheCategoryIsAccepted() throws InvalidBallotException {
            Ballot ballot = new SingleChoiceBallot("V001", "E1", "CAT1", List.of("C2"));
            ballot.validate(president);
            assertEquals(List.of("C2"), ballot.getChoices());
        }

        @Test
        void theIdMayBeTypedInAnyCaseAndIsStoredExactly() throws InvalidBallotException {
            Ballot ballot = new SingleChoiceBallot("V001", "E1", "CAT1", List.of("c1"));
            ballot.validate(president);
            assertEquals(List.of("C1"), ballot.getChoices());
        }

        @Test
        void noneOrTwoChoicesAreRefused() {
            Ballot none = new SingleChoiceBallot("V001", "E1", "CAT1", List.of());
            Ballot two = new SingleChoiceBallot("V001", "E1", "CAT1", List.of("C1", "C2"));

            assertThrows(InvalidBallotException.class, () -> none.validate(president));
            InvalidBallotException e = assertThrows(InvalidBallotException.class, () -> two.validate(president));
            assertEquals("Choose exactly 1 candidate for President", e.getMessage());
        }

        @Test
        void aCandidateOfAnotherCategoryIsRefused() {
            Ballot ballot = new SingleChoiceBallot("V001", "E1", "CAT1", List.of("C3"));

            InvalidBallotException e = assertThrows(InvalidBallotException.class, () -> ballot.validate(president));
            assertEquals("C3 is not a candidate for President", e.getMessage());
        }

        @Test
        void anUnknownIdIsRefused() {
            Ballot ballot = new SingleChoiceBallot("V001", "E1", "CAT1", List.of("C99"));
            assertThrows(InvalidBallotException.class, () -> ballot.validate(president));
        }
    }

    @Nested
    class SelectMultiple {

        @Test
        void exactlyAsManyChoicesAsSeatsIsAccepted() throws InvalidBallotException {
            Ballot ballot = new MultipleChoiceBallot("V001", "E1", "CAT2", List.of("C3", "C5"));
            ballot.validate(committee);
            assertEquals(List.of("C3", "C5"), ballot.getChoices());
        }

        @Test
        void fewerChoicesThanSeatsIsAccepted() throws InvalidBallotException {
            Ballot ballot = new MultipleChoiceBallot("V001", "E1", "CAT2", List.of("C4"));
            ballot.validate(committee);
            assertEquals(List.of("C4"), ballot.getChoices());
        }

        @Test
        void idsAreStoredExactlyAndInTheOrderTyped() throws InvalidBallotException {
            Ballot ballot = new MultipleChoiceBallot("V001", "E1", "CAT2", List.of("c5", "c3"));
            ballot.validate(committee);
            assertEquals(List.of("C5", "C3"), ballot.getChoices());
        }

        @Test
        void moreChoicesThanSeatsAreRefused() {
            Ballot ballot = new MultipleChoiceBallot("V001", "E1", "CAT2", List.of("C3", "C4", "C5"));

            InvalidBallotException e = assertThrows(InvalidBallotException.class, () -> ballot.validate(committee));
            assertEquals("Choose between 1 and 2 candidates for Committee", e.getMessage());
        }

        @Test
        void noChoiceIsRefused() {
            Ballot ballot = new MultipleChoiceBallot("V001", "E1", "CAT2", List.of());
            assertThrows(InvalidBallotException.class, () -> ballot.validate(committee));
        }

        @Test
        void theSameCandidateTwiceIsRefusedEvenInAnotherCase() {
            Ballot ballot = new MultipleChoiceBallot("V001", "E1", "CAT2", List.of("C3", "c3"));

            InvalidBallotException e = assertThrows(InvalidBallotException.class, () -> ballot.validate(committee));
            assertEquals("C3 was chosen twice", e.getMessage());
        }

        @Test
        void aCandidateOfAnotherCategoryIsRefused() {
            Ballot ballot = new MultipleChoiceBallot("V001", "E1", "CAT2", List.of("C3", "C1"));
            assertThrows(InvalidBallotException.class, () -> ballot.validate(committee));
        }
    }

    @Nested
    class YesNo {

        @ParameterizedTest
        @ValueSource(strings = {"YES", "yes", "Yes"})
        void yesInAnyCaseIsStoredAsYES(String answer) throws InvalidBallotException {
            Ballot ballot = new YesNoBallot("V001", "E1", "CAT3", List.of(answer));
            ballot.validate(feeRise);
            assertEquals(List.of("YES"), ballot.getChoices());
        }

        @Test
        void noInAnyCaseIsStoredAsNO() throws InvalidBallotException {
            Ballot ballot = new YesNoBallot("V001", "E1", "CAT3", List.of("nO"));
            ballot.validate(feeRise);
            assertEquals(List.of("NO"), ballot.getChoices());
        }

        @ParameterizedTest
        @ValueSource(strings = {"MAYBE", "Y", "N", "ja", ""})
        void anyOtherAnswerIsRefused(String answer) {
            Ballot ballot = new YesNoBallot("V001", "E1", "CAT3", List.of(answer));

            InvalidBallotException e = assertThrows(InvalidBallotException.class, () -> ballot.validate(feeRise));
            assertEquals("Answer YES or NO", e.getMessage());
        }

        @Test
        void noAnswerOrBothAnswersAreRefused() {
            Ballot none = new YesNoBallot("V001", "E1", "CAT3", List.of());
            Ballot both = new YesNoBallot("V001", "E1", "CAT3", List.of("YES", "NO"));

            assertThrows(InvalidBallotException.class, () -> none.validate(feeRise));
            assertThrows(InvalidBallotException.class, () -> both.validate(feeRise));
        }
    }

    @Test
    void aRefusedBallotKeepsWhatWasTyped() {
        Ballot ballot = new YesNoBallot("V001", "E1", "CAT3", List.of("maybe"));
        assertThrows(InvalidBallotException.class, () -> ballot.validate(feeRise));
        assertEquals(List.of("maybe"), ballot.getChoices());
    }
}
