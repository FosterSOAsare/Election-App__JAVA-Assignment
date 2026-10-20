package com.election.ui;

import com.election.model.Election;
import com.election.model.User;
import com.election.service.TallyService;

import java.util.List;
import java.util.Optional;
import java.util.Scanner;

public class AdminMenu implements Menu {

    private final User admin;
    private final TallyService tallyService;
    private final Scanner scanner;

    public AdminMenu(User admin, TallyService tallyService, Scanner scanner) {
        this.admin = admin;
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
            System.out.println("2. Manage an election");
            System.out.println("3. View election results");
            System.out.println("0. Exit");
            System.out.print("> ");

            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1", "2" -> System.out.println("Not built yet.");
                case "3" -> viewResults();
                case "0" -> running = false;
                default -> System.out.println("Unknown option: " + choice);
            }
        }
        System.out.println("Goodbye.");
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
