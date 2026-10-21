package com.election.ui;

import com.election.model.Election;
import com.election.model.ElectionStatus;
import com.election.model.User;
import com.election.service.ElectionManagementService;

import java.util.List;
import java.util.Optional;

/**
 * "Manage elections" for one admin. Only their own elections are shown:
 * drafts can be edited, and open elections can be closed.
 */
public class ManageElectionsMenu implements Menu {

    private final User admin;
    private final ElectionManagementService managementService;
    private final ConsoleInput input;
    private final ElectionView view = new ElectionView();

    public ManageElectionsMenu(User admin, ElectionManagementService managementService, ConsoleInput input) {
        this.admin = admin;
        this.managementService = managementService;
        this.input = input;
    }

    @Override
    public void run() {
        boolean running = true;
        while (running) {
            System.out.println();
            System.out.println("--- Manage elections ---");
            System.out.println("1. Edit a draft election");
            System.out.println("2. Close an election");
            System.out.println("0. Back");

            String choice = input.readLine("> ");
            switch (choice) {
                case "1" -> pickDraftToEdit();
                case "2" -> listElectionsToClose();
                case "0" -> running = false;
                default -> System.out.println("Unknown option: " + choice);
            }
        }
    }

    private void pickDraftToEdit() {
        List<Election> drafts = managementService.findAdminElections(admin, ElectionStatus.DRAFT);
        if (drafts.isEmpty()) {
            System.out.println("You have no draft elections. Only drafts can be edited.");
            return;
        }
        view.printElectionTable("Your draft elections:", drafts);

        String electionId = input.readLine("Election ID to edit (Enter to go back): ");
        if (electionId.isEmpty()) {
            return;
        }
        Optional<Election> selected = managementService.findAnElectionByAdmin(admin, ElectionStatus.DRAFT, electionId);
        if (selected.isEmpty()) {
            System.out.println("None of your drafts has the ID " + electionId);
            return;
        }
        editElection(selected.get());
    }

    private void editElection(Election election) {
        boolean editing = true;
        while (editing) {
            System.out.println();
            System.out.println("--- Edit " + election.getId() + " " + election.getName() + " ---");
            System.out.println("1. Edit election details");
            System.out.println("2. Edit categories");
            System.out.println("3. Edit candidates");
            System.out.println("0. Back");

            String choice = input.readLine("> ");
            switch (choice) {
                case "1", "2", "3" -> System.out.println("Not built yet.");
                case "0" -> editing = false;
                default -> System.out.println("Unknown option: " + choice);
            }
        }
    }

    private void listElectionsToClose() {
        List<Election> open = managementService.findAdminElections(admin, ElectionStatus.OPEN);
        if (open.isEmpty()) {
            System.out.println("You have no open elections. Only open elections can be closed.");
            return;
        }
        view.printElectionTable("Your open elections:", open);
        System.out.println("Closing is not built yet.");
    }
}
