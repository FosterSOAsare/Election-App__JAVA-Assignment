package com.election.ui;

import com.election.model.Deadlines;
import com.election.model.NameRules;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

/** Reads from the keyboard. Every method keeps asking until the answer is in the right shape. */
public class ConsoleInput {

    private final Scanner scanner;

    public ConsoleInput(Scanner scanner) {
        this.scanner = scanner;
    }

    public String readLine(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    /** Asks a yes/no question; only "yes" or "y" (any case) counts as yes. */
    public boolean confirm(String question) {
        String answer = readLine(question + " (yes/no): ");
        return answer.equalsIgnoreCase("yes") || answer.equalsIgnoreCase("y");
    }

    public String readRequired(String prompt, String emptyMessage) {
        while (true) {
            String text = readLine(prompt);
            if (!text.isEmpty()) {
                return text;
            }
            System.out.println(emptyMessage);
        }
    }

    /** A date with an optional time; see Deadlines for the accepted formats. No time means 00:00:00. */
    public LocalDateTime readDeadline(String prompt) {
        while (true) {
            String text = readLine(prompt);
            try {
                return Deadlines.parse(text);
            } catch (DateTimeParseException e) {
                System.out.println("Not a valid date and time: " + text + ". Use " + Deadlines.FORMAT_HINT + ".");
            }
        }
    }

    /** Like readDeadline, but pressing Enter returns empty, meaning "keep the current deadline". */
    public Optional<LocalDateTime> readOptionalDeadline(String prompt) {
        while (true) {
            String text = readLine(prompt);
            if (text.isEmpty()) {
                return Optional.empty();
            }
            try {
                return Optional.of(Deadlines.parse(text));
            } catch (DateTimeParseException e) {
                System.out.println("Not a valid date and time: " + text + ". Use " + Deadlines.FORMAT_HINT + ".");
            }
        }
    }

    public int readIntAtLeast(String prompt, int minimum) {
        while (true) {
            String text = readLine(prompt);
            try {
                int number = Integer.parseInt(text);
                if (number >= minimum) {
                    return number;
                }
            } catch (NumberFormatException e) {
                // Not a number: fall through to the message below and ask again.
            }
            System.out.println("Enter a whole number of " + minimum + " or more.");
        }
    }

    /**
     * A numbered list ending in "0. Back". Returns the index (from 0) of the option picked, or empty for Back.
     *
     * @param header printed above the options and lined up with them (a table header), or null for none
     */
    public Optional<Integer> pick(String title, String header, List<String> options) {
        int numberWidth = String.valueOf(options.size()).length();
        String numberRow = "  %" + numberWidth + "d. %s%n";
        while (true) {
            System.out.println(title);
            if (header != null) {
                System.out.println("  " + " ".repeat(numberWidth + 2) + header);
            }
            for (int i = 0; i < options.size(); i++) {
                System.out.printf(numberRow, i + 1, options.get(i));
            }
            System.out.printf(numberRow, 0, "Back");
            String text = readLine("> ");
            try {
                int choice = Integer.parseInt(text);
                if (choice == 0) {
                    return Optional.empty();
                }
                if (choice >= 1 && choice <= options.size()) {
                    return Optional.of(choice - 1);
                }
            } catch (NumberFormatException e) {
                // Not a number: fall through to the message below and ask again.
            }
            System.out.println("Choose a number from 0 to " + options.size() + ".");
        }
    }

    /** Prints the options as a numbered list and returns the index (from 0) of the one picked. */
    public int readChoice(String title, List<String> options) {
        while (true) {
            System.out.println(title);
            for (int i = 0; i < options.size(); i++) {
                System.out.println("  " + (i + 1) + ". " + options.get(i));
            }
            String text = readLine("> ");
            try {
                int choice = Integer.parseInt(text);
                if (choice >= 1 && choice <= options.size()) {
                    return choice - 1;
                }
            } catch (NumberFormatException e) {
                // Not a number: fall through to the message below and ask again.
            }
            System.out.println("Choose a number from 1 to " + options.size() + ".");
        }
    }

    /**
     * Reads a comma-separated list. Keeps asking until there is at least one name, no name twice,
     * and, if checkNameRules is true, every name passes NameRules.
     */
    public List<String> readNames(String prompt, boolean checkNameRules) {
        while (true) {
            List<String> names = Arrays.stream(readLine(prompt).split(","))
                    .map(String::trim)
                    .filter(n -> !n.isEmpty())
                    .toList();

            if (names.isEmpty()) {
                System.out.println("Enter at least one name.");
                continue;
            }
            long distinct = names.stream().map(String::toLowerCase).distinct().count();
            if (distinct != names.size()) {
                System.out.println("Each name can only be entered once.");
                continue;
            }
            if (checkNameRules) {
                Optional<String> invalid = names.stream()
                        .filter(n -> !NameRules.isValidName(n))
                        .findFirst();
                if (invalid.isPresent()) {
                    System.out.println("'" + invalid.get() + "' may only contain " + NameRules.ALLOWED_DESCRIPTION + ".");
                    continue;
                }
            }
            return names;
        }
    }
}
