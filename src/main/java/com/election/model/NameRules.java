package com.election.model;

import com.election.exception.InvalidNameException;

import java.util.regex.Pattern;

/** The one place that decides what a valid name looks like. */
public final class NameRules {

    /** Shown in error messages, so it must match the pattern below. */
    public static final String ALLOWED_DESCRIPTION = "letters, digits, spaces, hyphens (-), apostrophes (') and full stops (.)";

    // \p{L} is any letter in any language (so "Inès" is fine), \p{N} any digit.
    // Commas are never allowed: they separate CSV columns and the names typed in the console.
    private static final Pattern VALID_NAME = Pattern.compile("[\\p{L}\\p{N} .'-]+");

    private NameRules() {
    }

    public static boolean isValidName(String name) {
        return name != null && !name.isBlank() && VALID_NAME.matcher(name).matches();
    }

    /** @param what what the name belongs to, for the message, e.g. "Candidate name" */
    public static void requireValidName(String name, String what) throws InvalidNameException {
        if (!isValidName(name)) {
            throw new InvalidNameException(what + " '" + name + "' may only contain " + ALLOWED_DESCRIPTION);
        }
    }
}
