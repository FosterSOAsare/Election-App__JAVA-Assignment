package com.election.ui;

import com.election.exception.ElectionException;
import com.election.exception.InvalidBallotException;
import com.election.model.Ballot;
import com.election.model.Category;
import com.election.model.Election;
import com.election.model.User;
import com.election.service.TallyService;
import com.election.service.VotingService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** The voter's choices. Reading is done by ConsoleInput and printing by ElectionView. */
public class VoterMenu implements Menu {

    private final User voter;
    private final VotingService votingService;
    private final ConsoleInput input;
    private final ResultsScreen resultsScreen;
    private final ElectionView view = new ElectionView();

    public VoterMenu(User voter, VotingService votingService, TallyService tallyService, ConsoleInput input) {
        this.voter = voter;
        this.votingService = votingService;
        this.input = input;
        this.resultsScreen = new ResultsScreen(tallyService, input);
    }

    @Override
    public void run() {
        boolean running = true;
        while (running) {
            System.out.println();
            System.out.println("--- Voter menu (" + voter.getId() + ") ---");
            System.out.println("1. View open elections");
            System.out.println("2. Vote in an election");
            System.out.println("3. View election results");
            System.out.println("0. Log out");

            String choice = input.readLine("> ");
            switch (choice) {
                case "1" -> viewOpenElections();
                case "2" -> vote();
                case "3" -> resultsScreen.run();
                case "0" -> running = false;
                default -> System.out.println("Unknown option: " + choice);
            }
        }
        System.out.println("Logged out.");
    }

    /** Lists the open elections, then shows one election's categories and candidates, so voters can look first. */
    private void viewOpenElections() {
        List<Election> open = votingService.openElections();
        if (open.isEmpty()) {
            System.out.println("There are no open elections right now.");
            return;
        }
        view.printElectionTable("Open elections:", open);

        String electionId = input.readLine("Election ID to see its categories and candidates (Enter to go back): ");
        if (electionId.isEmpty()) {
            return;
        }
        Optional<Election> selected = votingService.findOpenElection(electionId);
        if (selected.isEmpty()) {
            System.out.println("No open election with ID " + electionId);
            return;
        }
        view.printElectionCategories(selected.get());
    }

    /** Asks for every category first and saves nothing until the voter confirms at the end. */
    private void vote() {
        List<Election> open = votingService.openElections();
        if (open.isEmpty()) {
            System.out.println("There are no open elections right now.");
            return;
        }
        view.printElectionTable("Open elections:", open);

        String electionId = input.readLine("Election ID to vote in (Enter to go back): ");
        if (electionId.isEmpty()) {
            return;
        }
        Optional<Election> selected = votingService.findOpenElection(electionId);
        if (selected.isEmpty()) {
            System.out.println("No open election with ID " + electionId);
            return;
        }
        Election election = selected.get();

        // Check for an existing vote before showing anything: one submission per voter per election.
        if (votingService.hasVoted(voter, election)) {
            System.out.println("You have already voted in " + election.getName() + ".");
            return;
        }
        System.out.println("Voting in " + election.getId() + " " + election.getName()
                + ". Press Enter to skip a category. You can review and change your answers before submitting.");

        // Category id -> the prepared ballot. A skipped category has no entry. Nothing here is saved yet.
        Map<String, Ballot> answers = new HashMap<>();
        for (Category category : election.getCategories()) {
            askVote(election, category).ifPresent(ballot -> answers.put(category.getId(), ballot));
        }
        reviewAndSubmit(election, answers);
    }

    /** Shows the answers and lets the voter submit, change one, or cancel, as often as they like. */
    private void reviewAndSubmit(Election election, Map<String, Ballot> answers) {
        List<Category> categories = election.getCategories();
        while (true) {
            view.printVoteSummary(election, answers);
            int next = input.readChoice("What next?",
                    List.of("Submit my votes", "Change an answer", "Cancel without saving"));

            if (next == 0) {
                if (answers.isEmpty()) {
                    System.out.println("You skipped every category, so there is nothing to submit.");
                    continue;
                }
                try {
                    votingService.submitBallots(voter, election, new ArrayList<>(answers.values()));
                    System.out.println("Your votes have been saved.");
                } catch (ElectionException e) {
                    System.out.println("Your votes were not saved: " + e.getMessage());
                }
                return;
            }
            if (next == 1) {
                List<String> names = categories.stream().map(Category::getName).toList();
                Category category = categories.get(input.readChoice("Which category?", names));
                // Forget the old answer first, so pressing Enter now leaves the category skipped.
                answers.remove(category.getId());
                askVote(election, category).ifPresent(ballot -> answers.put(category.getId(), ballot));
                continue;
            }
            System.out.println("Your votes were not saved.");
            return;
        }
    }

    /** One category: keeps asking until the answer is valid, or the voter presses Enter to skip it. */
    private Optional<Ballot> askVote(Election election, Category category) {
        System.out.println();
        view.printBallotPaper(category);
        while (true) {
            String answer = input.readLine(promptFor(category));
            if (answer.isEmpty()) {
                System.out.println("Skipped.");
                return Optional.empty();
            }
            List<String> choices = Arrays.stream(answer.split(","))
                    .map(String::trim)
                    .filter(c -> !c.isEmpty())
                    .toList();
            try {
                return Optional.of(votingService.prepareBallot(voter, election, category.getId(), choices));
            } catch (InvalidBallotException e) {
                System.out.println(e.getMessage() + ". Try again, or press Enter to skip.");
            } catch (ElectionException e) {
                // Not something a different answer can fix, such as a vote already cast here.
                System.out.println(e.getMessage() + ".");
                return Optional.empty();
            }
        }
    }

    private String promptFor(Category category) {
        return switch (category.getBallotType()) {
            case SELECT_ONE -> "Enter the ID of one candidate: ";
            case SELECT_MULTIPLE -> "Enter up to " + category.getSeats() + " candidate IDs, separated by commas: ";
            case YES_NO -> "Enter YES or NO: ";
        };
    }
}
