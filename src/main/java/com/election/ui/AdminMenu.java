package com.election.ui;

import com.election.model.Category;
import com.election.model.Election;
import com.election.model.User;
import com.election.service.ElectionManagementService;
import com.election.service.TallyService;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
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

        System.out.println("All elections:");
        for (Election election : elections) {
            System.out.println("  " + election.getId() + "  " + election.getName() + "  " + election.getDate()
                    + "  " + election.getStatus() + "  created by " + election.getCreatedBy());
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
        List<Category> categories = election.getCategories();
        if (categories.isEmpty()) {
            System.out.println(election.getName() + " has no categories yet.");
            return;
        }

        System.out.println("Categories in " + election.getName() + ":");
        for (Category category : categories) {
            System.out.println("  " + category.getId() + "  " + category.getName());
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

        List<String> categoryNames = readCategoryNames();

        Election election = managementService.createElection(admin, name, date, categoryNames);
        System.out.println("Created election:");
        System.out.println("  ID:         " + election.getId());
        System.out.println("  Name:       " + election.getName());
        System.out.println("  Date:       " + election.getDate());
        System.out.println("  Status:     " + election.getStatus());
        System.out.println("  Created by: " + election.getCreatedBy());
        System.out.println("  Categories:");
        for (Category category : election.getCategories()) {
            System.out.println("    " + category.getId() + "  " + category.getName());
        }
    }

    /** Keeps asking until the admin enters at least one category and no name twice. */
    private List<String> readCategoryNames() {
        while (true) {
            System.out.print("Categories, separated by commas (e.g. President, Treasurer): ");
            List<String> names = Arrays.stream(scanner.nextLine().split(","))
                    .map(String::trim)
                    .filter(n -> !n.isEmpty())
                    .toList();

            if (names.isEmpty()) {
                System.out.println("Enter at least one category.");
                continue;
            }
            long distinct = names.stream().map(String::toLowerCase).distinct().count();
            if (distinct != names.size()) {
                System.out.println("Each category can only be entered once.");
                continue;
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
