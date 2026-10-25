package com.election.ui;

import com.election.model.Deadlines;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** ConsoleInput reads from a Scanner, so a test can type its answers as a string instead of a keyboard. */
class ConsoleInputTest {

    private final PrintStream realOut = System.out;
    private final ByteArrayOutputStream printed = new ByteArrayOutputStream();

    @BeforeEach
    void capturePrinting() {
        System.setOut(new PrintStream(printed, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    void restorePrinting() {
        System.setOut(realOut);
    }

    /** One typed line per argument. */
    private static ConsoleInput typing(String... lines) {
        return new ConsoleInput(new Scanner(String.join("\n", lines) + "\n"));
    }

    private String printed() {
        return printed.toString(StandardCharsets.UTF_8);
    }

    private int timesPrinted(String text) {
        return printed().split(java.util.regex.Pattern.quote(text), -1).length - 1;
    }

    @Test
    void readLinePrintsThePromptAndTrimsTheAnswer() {
        assertEquals("V001", typing("   V001  ").readLine("Enter your user ID: "));
        assertTrue(printed().contains("Enter your user ID: "));
    }

    @Nested
    class Confirm {

        @Test
        void yesYAndNoNInAnyCaseAreAccepted() {
            assertTrue(typing("yes").confirm("Close it?"));
            assertTrue(typing("Y").confirm("Close it?"));
            assertFalse(typing("NO").confirm("Close it?"));
            assertFalse(typing("n").confirm("Close it?"));
        }

        @Test
        void anythingElseAsksAgainInsteadOfCountingAsNo() {
            assertTrue(typing("yse", "maybe", "", "y").confirm("Close it?"));
            assertEquals(3, timesPrinted("Please answer yes or no."));
        }
    }

    @Nested
    class Pick {

        private final List<String> options = List.of("E1  Student Council", "E5  Graduation Speaker");

        @Test
        void aNumberPicksTheMatchingOption() {
            assertEquals(Optional.of(1), typing("2").pick("Open elections:", null, options));
        }

        @Test
        void zeroGoesBack() {
            assertEquals(Optional.empty(), typing("0").pick("Open elections:", null, options));
            assertTrue(printed().contains("0. Back"));
        }

        @Test
        void aNumberOutsideTheListOrTextAsksAgain() {
            assertEquals(Optional.of(0), typing("3", "-1", "x", "", "1").pick("Open elections:", null, options));
            assertEquals(4, timesPrinted("Choose a number from 0 to 2."));
        }

        @Test
        void theHeaderIsPrintedAboveTheOptions() {
            typing("0").pick("Open elections:", "ID  Name", options);
            assertTrue(printed().contains("ID  Name"));
        }
    }

    @Nested
    class Choice {

        @Test
        void readChoiceHasNoZeroOption() {
            assertEquals(0, typing("0", "1").readChoice("What next?", List.of("Submit", "Cancel")));
            assertTrue(printed().contains("Choose a number from 1 to 2."));
        }

        @Test
        void readChoiceOrCancelOffersZeroToCancel() {
            assertEquals(2, typing("3").readChoiceOrCancel("Type:", List.of("Select one", "Select multiple", "Yes / No")));
            assertThrows(CancelledException.class,
                    () -> typing("0").readChoiceOrCancel("Type:", List.of("Select one", "Yes / No")));
            assertTrue(printed().contains("0. Cancel"));
        }
    }

    @Nested
    class FlowQuestions {

        @Test
        void readRequiredAsksAgainForAnEmptyAnswer() {
            assertEquals("Chess Club", typing("", "  ", "Chess Club").readRequired("Name: ", "The name cannot be empty."));
            assertEquals(2, timesPrinted("The name cannot be empty."));
        }

        @Test
        void readDeadlineAsksAgainUntilTheDateIsValid() {
            assertEquals(Deadlines.parse("2026-11-10 18:00"),
                    typing("", "soon", "2026-02-30", "2026-11-10 18:00").readDeadline("Deadline: "));
            assertEquals(1, timesPrinted("Enter a deadline"));
            assertEquals(2, timesPrinted("Not a valid date and time"));
        }

        @Test
        void readOptionalDeadlineTreatsEnterAsKeep() {
            assertEquals(Optional.empty(), typing("").readOptionalDeadline("New deadline: "));
            assertEquals(Optional.of(Deadlines.parse("2027-01-01")),
                    typing("bad", "2027-01-01").readOptionalDeadline("New deadline: "));
        }

        @Test
        void readIntAtLeastAsksAgainBelowTheMinimumOrForText() {
            assertEquals(3, typing("1", "x", "", "3").readIntAtLeast("Seats: ", 2));
            assertEquals(3, timesPrinted("Enter a whole number of 2 or more."));
        }

        @Test
        void readNamesSplitsOnCommasTrimsAndDropsEmptyEntries() {
            assertEquals(List.of("Ana", "Ben"), typing("  Ana , , Ben ,").readNames("Names: ", true));
        }

        @Test
        void readNamesAsksAgainForNoNamesARepeatedNameOrAnInvalidName() {
            assertEquals(List.of("Ana", "Ben"), typing(",,", "Ana, ana", "Ana, B@n", "Ana, Ben").readNames("Names: ", true));
            assertEquals(1, timesPrinted("Enter at least one name."));
            assertEquals(1, timesPrinted("Each name can only be entered once."));
            assertEquals(1, timesPrinted("'B@n' may only contain"));
        }

        @Test
        void readNamesWithoutNameRulesAllowsQuestionMarks() {
            assertEquals(List.of("Raise the fee?"), typing("Raise the fee?").readNames("Categories: ", false));
        }

        @Test
        void zeroCancelsTheWholeFlowFromAnyOfTheseQuestions() {
            assertThrows(CancelledException.class, () -> typing("0").readRequired("Name: ", "Empty"));
            assertThrows(CancelledException.class, () -> typing("0").readDeadline("Deadline: "));
            assertThrows(CancelledException.class, () -> typing("0").readIntAtLeast("Seats: ", 2));
            assertThrows(CancelledException.class, () -> typing("0").readNames("Names: ", true));
        }

        @Test
        void zeroOnlyCancelsWhenItIsTheWholeAnswer() {
            assertEquals(List.of("Agent 0", "Ben"), typing("Agent 0, Ben").readNames("Names: ", true));
        }
    }
}
