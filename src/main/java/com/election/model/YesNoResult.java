package com.election.model;

import java.util.Locale;
import java.util.Map;

/** Result of a Yes / No category. It passes only when yes votes outnumber no votes, so a tie is rejected. */
public class YesNoResult extends CategoryResult {

    public static final String YES = "YES";
    public static final String NO = "NO";

    public YesNoResult(Category category, int ballotCount, int yes, int no) {
        super(category, ballotCount, Map.of(YES, yes, NO, no));
    }

    public int getYes() {
        return votesFor(YES);
    }

    public int getNo() {
        return votesFor(NO);
    }

    public boolean isPassed() {
        return getYes() > getNo();
    }

    @Override
    public String summary() {
        int total = getYes() + getNo();
        String line = "YES " + getYes() + "   NO " + getNo() + System.lineSeparator();
        if (total == 0) {
            return line + "Rejected (no votes)";
        }
        // Locale.ROOT always prints a decimal point, so it reads 70.0 and not 70,0 on a French or German system.
        String percent = String.format(Locale.ROOT, "%.1f%% yes", 100.0 * getYes() / total);
        return line + (isPassed() ? "Passed (" : "Rejected (") + percent + ")";
    }
}
