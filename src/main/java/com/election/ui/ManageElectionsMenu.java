package com.election.ui;

import com.election.exception.ElectionException;
import com.election.model.Deadlines;
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
    private final ElectionPicker picker;

    public ManageElectionsMenu(User admin, ElectionManagementService managementService, ConsoleInput input) {
        this.admin = admin;
        this.managementService = managementService;
        this.input = input;
        this.picker = new ElectionPicker(input);
    }

    @Override
    public void run() {
        boolean running = true;
        while (running) {
            System.out.println();
            System.out.println("--- Manage elections ---");
            System.out.println("1. Manage a draft");
            System.out.println("2. Close an election");
            System.out.println("0. Back");

            String choice = input.readLine("> ");
            switch (choice) {
                case "1" -> pickDraftToManage();
                case "2" -> pickElectionToClose();
                case "0" -> running = false;
                default -> System.out.println("Unknown option: " + choice);
            }
        }
    }

    private void pickDraftToManage() {
        List<Election> drafts = managementService.findAdminElections(admin, ElectionStatus.DRAFT);
        if (drafts.isEmpty()) {
            System.out.println("You have no draft elections. Only drafts can be managed.");
            return;
        }
        Optional<Election> selected = picker.pick("Your draft elections (pick one to manage):", drafts);
        if (selected.isEmpty()) {
            return;
        }
        manageDraft(selected.get());
    }

    private void manageDraft(Election election) {
        boolean managing = true;
        while (managing) {
            System.out.println();
            System.out.println("--- Manage draft " + election.getId() + " " + election.getName() + " ---");
            System.out.println("1. Edit election details");
            System.out.println("2. Open voting");
            System.out.println("3. Delete this draft");
            System.out.println("0. Back");

            String choice = input.readLine("> ");
            switch (choice) {
                case "1" -> new EditDraftMenu(admin, election, managementService, input).run();
                // Once it is open, or deleted, it is no longer a draft, so this menu ends.
                case "2" -> managing = !openVoting(election);
                case "3" -> managing = !deleteDraft(election);
                case "0" -> managing = false;
                default -> System.out.println("Unknown option: " + choice);
            }
        }
    }

    /** Returns true if the draft was deleted. */
    private boolean deleteDraft(Election election) {
        int categories = election.getCategories().size();
        int candidates = election.getCategories().stream().mapToInt(c -> c.getCandidates().size()).sum();
        String question = "Delete " + election.getId() + " " + election.getName() + " with its " + categories
                + (categories == 1 ? " category" : " categories") + " and " + candidates
                + (candidates == 1 ? " candidate" : " candidates") + "? This cannot be undone.";
        if (!input.confirm(question)) {
            System.out.println(election.getId() + " was not deleted.");
            return false;
        }
        try {
            managementService.deleteElection(admin, election);
            System.out.println("Deleted " + election.getId() + " " + election.getName() + ".");
            return true;
        } catch (ElectionException e) {
            System.out.println("Could not delete " + election.getId() + ": " + e.getMessage() + ".");
            return false;
        }
    }

    /** Returns true if the election was opened. */
    private boolean openVoting(Election election) {
        // After opening, the draft can no longer be edited, so ask first.
        System.out.println("Opening " + election.getId() + " " + election.getName()
                + " lets voters vote and freezes its details, categories and candidates.");
        String confirm = input.readLine("Open it? (yes/no): ");
        if (!confirm.equalsIgnoreCase("yes") && !confirm.equalsIgnoreCase("y")) {
            System.out.println(election.getId() + " is still a draft.");
            return false;
        }

        try {
            managementService.openElection(admin, election);
            System.out.println(election.getId() + " is now " + election.getStatus() + ". Voters can vote in it.");
            return true;
        } catch (ElectionException e) {
            System.out.println("Could not open " + election.getId() + ": " + e.getMessage());
            return false;
        }
    }

    private void pickElectionToClose() {
        List<Election> open = managementService.findAdminElections(admin, ElectionStatus.OPEN);
        if (open.isEmpty()) {
            System.out.println("You have no open elections. Only open elections can be closed.");
            return;
        }
        Optional<Election> selected = picker.pick("Your open elections (pick one to close):", open);
        if (selected.isEmpty()) {
            return;
        }
        Election election = selected.get();

        // Closing cannot be undone, so ask first.
        System.out.println("Closing " + election.getId() + " " + election.getName()
                + " ends voting for good and makes its results available.");
        if (!managementService.deadlinePassed(election)) {
            System.out.println("Note: its deadline (" + Deadlines.format(election.getDeadline()) + ") has not passed yet. Closing early is"
                    + " allowed for testing; in a real election it could only be closed after the deadline.");
        }
        String confirm = input.readLine("Close it? (yes/no): ");
        if (!confirm.equalsIgnoreCase("yes") && !confirm.equalsIgnoreCase("y")) {
            System.out.println(election.getId() + " is still open.");
            return;
        }

        try {
            managementService.closeElection(admin, election);
            System.out.println(election.getId() + " is now " + election.getStatus()
                    + ". Its results are under View election results.");
        } catch (ElectionException e) {
            System.out.println("Could not close " + election.getId() + ": " + e.getMessage());
        }
    }
}
