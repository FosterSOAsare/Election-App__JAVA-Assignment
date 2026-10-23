package com.election.ui;

import com.election.exception.InvalidNameException;
import com.election.model.Deadlines;
import com.election.model.Election;
import com.election.model.User;
import com.election.service.ElectionManagementService;
import com.election.service.ElectionManagementService.NewCategory;
import com.election.service.TallyService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** The admin's choices. Reading is done by ConsoleInput and printing by ElectionView. */
public class AdminMenu implements Menu {

    private final User admin;
    private final ElectionManagementService managementService;
    private final ConsoleInput input;
    private final ManageElectionsMenu manageMenu;
    private final ResultsScreen resultsScreen;
    private final ElectionView view = new ElectionView();

    public AdminMenu(User admin, ElectionManagementService managementService, TallyService tallyService,
                     ConsoleInput input) {
        this.admin = admin;
        this.managementService = managementService;
        this.input = input;
        this.manageMenu = new ManageElectionsMenu(admin, managementService, input);
        this.resultsScreen = new ResultsScreen(tallyService, input);
    }

    @Override
    public void run() {
        boolean running = true;
        while (running) {
            System.out.println();
            System.out.println("--- Admin menu (" + admin.getId() + ") ---");
            System.out.println("1. Create election");
            System.out.println("2. View all elections");
            System.out.println("3. Manage elections");
            System.out.println("4. View election results");
            System.out.println("0. Log out");

            String choice = input.readLine("> ");
            switch (choice) {
                case "1" -> createElection();
                case "2" -> viewAllElections();
                case "3" -> manageMenu.run();
                case "4" -> resultsScreen.run();
                case "0" -> running = false;
                default -> System.out.println("Unknown option: " + choice);
            }
        }
        System.out.println("Logged out.");
    }

    private void viewAllElections() {
        List<Election> elections = managementService.allElections();
        if (elections.isEmpty()) {
            System.out.println("There are no elections yet.");
            return;
        }
        Optional<Election> selected = new ElectionPicker(input).pick("All elections (pick one to see it):",
                elections);
        if (selected.isEmpty()) {
            return;
        }
        view.printElection(selected.get());
        view.printTurnout(selected.get());
    }

    private void createElection() {
        String name = input.readRequired("Election name: ", "The name cannot be empty.");
        System.out.println("The deadline is the moment voting ends: votes are accepted until then,");
        System.out.println("and the election cannot be opened once it has passed. Without a time it is 00:00:00,");
        System.out.println("the very start of that day.");
        LocalDateTime deadline = input.readDeadline("Voting deadline (" + Deadlines.FORMAT_HINT + "): ");
        List<NewCategory> categories = new CategoryForm(input).askCategories();

        try {
            Election election = managementService.createElection(admin, name, deadline, categories);
            view.printCreatedElection(election);
        } catch (InvalidNameException e) {
            System.out.println("Could not create election: " + e.getMessage());
        }
    }
}
