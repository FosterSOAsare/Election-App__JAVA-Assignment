package com.election.ui;

import com.election.model.Election;
import com.election.model.User;
import com.election.service.ElectionManagementService;
import com.election.service.TallyService;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
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

        Election election = managementService.createElection(admin, name, date);
        System.out.println("Created election:");
        System.out.println("  ID:         " + election.getId());
        System.out.println("  Name:       " + election.getName());
        System.out.println("  Date:       " + election.getDate());
        System.out.println("  Status:     " + election.getStatus());
        System.out.println("  Created by: " + election.getCreatedBy());
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
