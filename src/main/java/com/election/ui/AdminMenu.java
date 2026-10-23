package com.election.ui;

import com.election.exception.ElectionException;
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

    /** Typing 0 at any question cancels it; nothing is saved until every question has been answered. */
    private void createElection() {
        try {
            String name = askElectionName();
            System.out.println("The deadline is the moment voting ends: votes are accepted until then,");
            System.out.println("and the election cannot be opened once it has passed. Without a time it is 00:00:00,");
            System.out.println("the very start of that day.");
            LocalDateTime deadline = askFutureDeadline();
            List<NewCategory> categories = new CategoryForm(input).askCategories();

            Election election = managementService.createElection(admin, name, deadline, categories);
            view.printCreatedElection(election);
        } catch (CancelledException e) {
            System.out.println("Creation cancelled. Nothing was saved.");
        } catch (ElectionException e) {
            System.out.println("Could not create election: " + e.getMessage());
        }
    }

    /** Asks again until the name is not empty and has no comma (a comma would split the CSV row). */
    private String askElectionName() {
        while (true) {
            String name = input.readRequired("Election name (0 to cancel): ", "The name cannot be empty.");
            if (!name.contains(",")) {
                return name;
            }
            System.out.println("An election name cannot contain a comma.");
        }
    }

    /** Asks again until the deadline is a valid date and time that has not passed yet. */
    private LocalDateTime askFutureDeadline() {
        while (true) {
            LocalDateTime deadline = input.readDeadline("Voting deadline (" + Deadlines.FORMAT_HINT + "; 0 to cancel): ");
            if (!managementService.deadlinePassed(deadline)) {
                return deadline;
            }
            System.out.println("That deadline has already passed. Enter one in the future.");
        }
    }
}
