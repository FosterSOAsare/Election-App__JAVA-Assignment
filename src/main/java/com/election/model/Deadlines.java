package com.election.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoField;

/**
 * Reads and prints election deadlines, so the CSV file and the console accept exactly the same text:
 * "2026-11-10", "2026-11-10 18:00" or "2026-11-10 18:00:30". A missing time means 00:00:00.
 */
public final class Deadlines {

    public static final String FORMAT_HINT = "YYYY-MM-DD, or YYYY-MM-DD HH:MM[:SS]";

    // [ ... ] marks an optional part; parseDefaulting fills in whatever was left out.
    // STRICT with "uuuu" refuses impossible dates such as 2026-02-30 instead of quietly changing them.
    private static final DateTimeFormatter INPUT = new DateTimeFormatterBuilder()
            .appendPattern("uuuu-MM-dd[ HH:mm[:ss]]")
            .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
            .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
            .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
            .toFormatter()
            .withResolverStyle(ResolverStyle.STRICT);

    private static final DateTimeFormatter OUTPUT = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss");

    private Deadlines() {
    }

    /** @throws java.time.format.DateTimeParseException if the text is not in one of the accepted formats */
    public static LocalDateTime parse(String text) {
        return LocalDateTime.parse(text.trim(), INPUT);
    }

    public static String format(LocalDateTime deadline) {
        return deadline.format(OUTPUT);
    }
}
