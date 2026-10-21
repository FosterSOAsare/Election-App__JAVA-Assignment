package com.election.ui;

import com.election.exception.InvalidNameException;
import com.election.model.Candidate;
import com.election.model.Category;
import com.election.model.Election;
import com.election.model.NameRules;
import com.election.model.User;
import com.election.service.ElectionManagementService;
import com.election.service.TallyService;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;

public class AdminMenu implements Menu {

    private final User admin;
    private final ElectionManagementService managementService;
    private final TallyService tallyService;
    private final Scanner scanner;

    public AdminMenu(User admin, ElectionManagementService managementService, TallyService tallyService,
                     Scanner scanner) {
        this.admin = admin;
        this.managementService = managementService;
        this.tallyService = tallyService;
        this.scanner = scanner;
    }

    @Override
    public void run() {
        boolean running = true;
        while (running) {
            System.out.println();
            System.out.println("--- Admin menu (" + admin.getId() + ") ---");
            System.out.println("1. Create election");
            System.out.println("2. View all elections");
            System.out.println("3. Manage an election");
            System.out.println("4. View election results");
            System.out.println("0. Exit");
            System.out.print("> ");

            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1" -> createElection();
                case "2" -> viewAllElections();
                case "3" -> System.out.println("Not built yet.");
                case "4" -> viewResults();
                case "0" -> running = false;
                default -> System.out.println("Unknown option: " + choice);
            }
        }
        System.out.println("Goodbye.");
    }

    private void viewAllElections() {
        List<Election> elections = managementService.allElections();
        if (elections.isEmpty()) {
            System.out.println("There are no elections yet.");
            return;
        }

        // Pad each column to its longest value so the table lines up.
        int idWidth = Math.max("ID".length(), elections.stream().mapToInt(e -> e.getId().length()).max().orElse(0));
        int nameWidth = Math.max("Name".length(),
                elections.stream().mapToInt(e -> e.getName().length()).max().orElse(0));
        String row = "  %-" + idWidth + "s  %-" + nameWidth + "s  %-10s  %-6s  %s%n";

        System.out.println("All elections:");
        System.out.printf(row, "ID", "Name", "Date", "Status", "Created by");
        for (Election election : elections) {
            System.out.printf(row, election.getId(), election.getName(), election.getDate(),
                    election.getStatus(), election.getCreatedBy());
        }

        System.out.print("Election ID to see its categories (Enter to go back): ");
        String electionId = scanner.nextLine().trim();
        if (electionId.isEmpty()) {
            return;
        }

        Optional<Election> selected = managementService.findElection(electionId);
        if (selected.isEmpty()) {
            System.out.println("Unknown election ID: " + electionId);
            return;
        }

        Election election = selected.get();
        if (election.getCategories().isEmpty()) {
            System.out.println(election.getName() + " has no categories yet.");
            return;
        }

        System.out.println();
        System.out.println("Categories in " + election.getId() + " " + election.getName() + ":");
        printCategories(election, "  ");
    }

    /**
     * Prints each category with its candidates indented underneath, for example:
     *   CAT1  President
     *         C1  Amara Mensah
     *         C2  Kofi Boateng
     * Id widths are taken from the longest id in the whole election, so every category lines up.
     */
    private void printCategories(Election election, String indent) {
        List<Category> categories = election.getCategories();

        int categoryIdWidth = categories.stream().mapToInt(c -> c.getId().length()).max().orElse(0);
        int candidateIdWidth = categories.stream()
                .flatMap(c -> c.getCandidates().stream())
                .mapToInt(c -> c.getId().length())
                .max()
                .orElse(0);

        // Candidates start under the category name, not under its id.
        String candidateIndent = indent + " ".repeat(categoryIdWidth + 2);
        String categoryRow = indent + "%-" + categoryIdWidth + "s  %s%n";
        String candidateRow = candidateIndent + "%-" + candidateIdWidth + "s  %s%n";

        for (Category category : categories) {
            System.out.printf(categoryRow, category.getId(), category.getName());
            if (category.getCandidates().isEmpty()) {
                System.out.println(candidateIndent + "(no candidates)");
            }
            for (Candidate candidate : category.getCandidates()) {
                System.out.printf(candidateRow, candidate.getId(), candidate.getName());
            }
        }
    }

    private void createElection() {
        String name = "";
        while (name.isEmpty()) {
            System.out.print("Election name: ");
            name = scanner.nextLine().trim();
            if (name.isEmpty()) {
                System.out.println("The name cannot be empty.");
            }
        }

        // Keep asking until the date is in the right format.
        LocalDate date = null;
        while (date == null) {
            System.out.print("Election date (YYYY-MM-DD): ");
            String text = scanner.nextLine().trim();
            try {
                date = LocalDate.parse(text);
            } catch (DateTimeParseException e) {
                System.out.println("Not a valid date: " + text);
            }
        }

        List<String> categoryNames = readNames("Categories, separated by commas (e.g. President, Treasurer): ",
                false);

        // LinkedHashMap keeps the categories in the order they were typed.
        Map<String, List<String>> candidatesByCategory = new LinkedHashMap<>();
        for (String categoryName : categoryNames) {
            candidatesByCategory.put(categoryName,
                    readNames("Candidates for " + categoryName + ", separated by commas: ", true));
        }

        Election election;
        try {
            election = managementService.createElection(admin, name, date, candidatesByCategory);
        } catch (InvalidNameException e) {
            System.out.println("Could not create election: " + e.getMessage());
            return;
        }
        System.out.println("Created election:");
        System.out.println("  ID:         " + election.getId());
        System.out.println("  Name:       " + election.getName());
        System.out.println("  Date:       " + election.getDate());
        System.out.println("  Status:     " + election.getStatus());
        System.out.println("  Created by: " + election.getCreatedBy());
        System.out.println("  Categories:");
        printCategories(election, "    ");
    }

    /**
     * Reads a comma-separated list. Keeps asking until there is at least one name, no name twice,
     * and, if checkNameRules is true, every name passes NameRules.
     */
    private List<String> readNames(String prompt, boolean checkNameRules) {
        while (true) {
            System.out.print(prompt);
            List<String> names = Arrays.stream(scanner.nextLine().split(","))
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

    private void viewResults() {
        List<Election> closed = tallyService.closedElections();
        if (closed.isEmpty()) {
            System.out.println("No elections have been closed yet.");
            return;
        }

        System.out.println("Closed elections:");
        for (Election election : closed) {
            System.out.println("  " + election.getId() + "  " + election.getName() + "  " + election.getDate());
        }

        System.out.print("Election ID to view: ");
        String electionId = scanner.nextLine().trim();

        Optional<Election> selected = tallyService.findClosedElection(electionId);
        if (selected.isEmpty()) {
            System.out.println("No closed election with ID " + electionId);
            return;
        }

        System.out.println("Results for " + selected.get().getName() + ": not built yet.");
    }
}
