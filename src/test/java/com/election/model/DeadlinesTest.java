package com.election.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DeadlinesTest {

    // --- happy paths

    @Test
    void aDateWithoutATimeMeansMidnight() {
        assertEquals(LocalDateTime.of(2026, 11, 10, 0, 0, 0), Deadlines.parse("2026-11-10"));
    }

    @Test
    void hoursAndMinutesAreRead() {
        assertEquals(LocalDateTime.of(2026, 11, 10, 18, 0, 0), Deadlines.parse("2026-11-10 18:00"));
    }

    @Test
    void secondsAreRead() {
        assertEquals(LocalDateTime.of(2026, 11, 10, 18, 0, 30), Deadlines.parse("2026-11-10 18:00:30"));
    }

    @Test
    void formatAlwaysPrintsSeconds() {
        assertEquals("2026-11-10 18:00:00", Deadlines.format(LocalDateTime.of(2026, 11, 10, 18, 0)));
    }

    @Test
    void whatIsPrintedCanBeReadBackUnchanged() {
        LocalDateTime deadline = LocalDateTime.of(2026, 11, 10, 18, 5, 9);
        assertEquals(deadline, Deadlines.parse(Deadlines.format(deadline)));
    }

    // --- boundaries

    @Test
    void spacesAroundTheTextAreIgnored() {
        assertEquals(LocalDateTime.of(2026, 11, 10, 0, 0, 0), Deadlines.parse("  2026-11-10  "));
    }

    @Test
    void theLastSecondOfADayIsValid() {
        assertEquals(LocalDateTime.of(2026, 12, 31, 23, 59, 59), Deadlines.parse("2026-12-31 23:59:59"));
    }

    @Test
    void aLeapDayIsValidOnlyInALeapYear() {
        assertEquals(LocalDateTime.of(2028, 2, 29, 0, 0, 0), Deadlines.parse("2028-02-29"));
        assertThrows(DateTimeParseException.class, () -> Deadlines.parse("2027-02-29"));
    }

    // --- errors

    @ParameterizedTest
    @ValueSource(strings = {"2026-02-30", "2026-04-31", "2026-13-01", "2026-00-10"})
    void datesThatDoNotExistAreRejected(String text) {
        assertThrows(DateTimeParseException.class, () -> Deadlines.parse(text));
    }

    @ParameterizedTest
    @ValueSource(strings = {"2026-11-10 24:00", "2026-11-10 25:00", "2026-11-10 18:60", "2026-11-10 18:00:60"})
    void timesThatDoNotExistAreRejected(String text) {
        assertThrows(DateTimeParseException.class, () -> Deadlines.parse(text));
    }

    @ParameterizedTest
    @ValueSource(strings = {"10/11/2026", "2026-1-5", "2026-11-10T18:00", "2026-11-10 18", "2026-11-10 6:00",
            "soon", ""})
    void otherFormatsAreRejected(String text) {
        assertThrows(DateTimeParseException.class, () -> Deadlines.parse(text));
    }
}
