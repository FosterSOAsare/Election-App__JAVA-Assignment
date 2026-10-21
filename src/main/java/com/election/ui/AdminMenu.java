package com.election.ui;

import com.election.exception.InvalidNameException;
import com.election.model.BallotType;
import com.election.model.Election;
import com.election.model.User;
import com.election.service.ElectionManagementService;
import com.election.service.ElectionManagementService.NewCategory;
import com.election.service.TallyService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/** The admin's choices. Reading is done by ConsoleInput and printing by ElectionView. */
public class AdminMenu implements Menu {

    private final User admin;
    private final ElectionManagementService managementService;
    private final ConsoleInput input;
    private final ResultsScreen resultsScreen;
    private final ElectionView view = new ElectionView();

    public AdminMenu(User admin, ElectionManagementService managementService, TallyService tallyService,
                     ConsoleInput input) {
        this.admin = admin;
        this.managementService = managementService;
        this.input = input;
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
            System.out.println("3. Manage an election");
            System.out.println("4. View election results");
            System.out.println("0. Log out");

            String choice = input.readLine("> ");
            switch (choice) {
                case "1" -> createElection();
                case "2" -> viewAllElections();
                case "3" -> System.out.println("Not built yet.");
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
        view.printElectionTable("All elections:", elections);

        String electionId = input.readLine("Election ID to see its categories and ballots (Enter to go back): ");
        if (electionId.isEmpty()) {
            return;
        }

        Optional<Election> selected = managementService.findElection(electionId);
        if (selected.isEmpty()) {
            System.out.println("Unknown election ID: " + electionId);
            return;
        }
        view.printElectionCategories(selected.get());
        view.printBallots(selected.get());
    }

    private void createElection() {
        String name = input.readRequired("Election name: ", "The name cannot be empty.");
        LocalDate date = input.readDate("Election date (YYYY-MM-DD): ");
        List<String> categoryNames = input.readNames(
                "Categories, separated by commas (e.g. President, Treasurer): ", false);

        List<NewCategory> categories = new ArrayList<>();
        for (String categoryName : categoryNames) {
            categories.add(askCategoryDetails(categoryName));
        }

        try {
            Election election = managementService.createElection(admin, name, date, categories);
            view.printCreatedElection(election);
        } catch (InvalidNameException e) {
            System.out.println("Could not create election: " + e.getMessage());
        }
    }

    /** Asks for one category's type first, then seats (multiple only), then candidates (not for yes/no). */
    private NewCategory askCategoryDetails(String categoryName) {
        System.out.println();
        List<String> typeLabels = Arrays.stream(BallotType.values()).map(BallotType::getLabel).toList();
        BallotType ballotType = BallotType.values()[input.readChoice("Type for " + categoryName + ":", typeLabels)];

        int seats = ballotType == BallotType.SELECT_MULTIPLE
                ? input.readIntAtLeast("How many winners for " + categoryName + "? (2 or more): ", 2)
                : 1;

        List<String> candidateNames = ballotType == BallotType.YES_NO
                ? List.of()
                : askCandidateNames(categoryName, ballotType, seats);

        return new NewCategory(categoryName, ballotType, seats, candidateNames);
    }

    /** A select-multiple category needs more candidates than winners, or there would be nothing to choose. */
    private List<String> askCandidateNames(String categoryName, BallotType ballotType, int seats) {
        while (true) {
            List<String> names = input.readNames("Candidates for " + categoryName + ", separated by commas: ", true);
            if (ballotType != BallotType.SELECT_MULTIPLE || names.size() > seats) {
                return names;
            }
            System.out.println("With " + seats + " winners you need at least " + (seats + 1) + " candidates.");
        }
    }
}
