package com.election.ui;

import com.election.exception.ElectionException;
import com.election.model.BallotType;
import com.election.model.Candidate;
import com.election.model.Category;
import com.election.model.Election;
import com.election.model.NameRules;
import com.election.model.User;
import com.election.service.ElectionManagementService;

import java.util.List;
import java.util.Optional;

/**
 * Editing one draft election: its own details, its categories (and each category's candidates),
 * and adding new categories. Only drafts get here, so nothing can change once voting has started.
 */
public class EditDraftMenu implements Menu {

    private final User admin;
    private final Election election;
    private final ElectionManagementService managementService;
    private final ConsoleInput input;

    public EditDraftMenu(User admin, Election election, ElectionManagementService managementService,
                         ConsoleInput input) {
        this.admin = admin;
        this.election = election;
        this.managementService = managementService;
        this.input = input;
    }

    @Override
    public void run() {
        boolean editing = true;
        while (editing) {
            System.out.println();
            System.out.println("--- Edit " + election.getId() + " " + election.getName() + " ---");
            System.out.println("1. Election details (name, deadline)");
            System.out.println("2. Edit categories");
            System.out.println("3. Add categories");
            System.out.println("0. Back");

            String choice = input.readLine("> ");
            switch (choice) {
                case "1", "3" -> System.out.println("Not built yet.");
                case "2" -> pickCategoryToEdit();
                case "0" -> editing = false;
                default -> System.out.println("Unknown option: " + choice);
            }
        }
    }

    private void pickCategoryToEdit() {
        List<Category> categories = election.getCategories();
        if (categories.isEmpty()) {
            System.out.println(election.getName() + " has no categories yet. Add one first.");
            return;
        }
        List<String> labels = categories.stream()
                .map(c -> c.getId() + "  " + c.getName() + "  [" + ElectionView.typeDescription(c) + "]")
                .toList();
        Category category = categories.get(input.readChoice("Which category?", labels));
        editCategory(category);
    }

    private void editCategory(Category category) {
        boolean editing = true;
        while (editing) {
            System.out.println();
            System.out.println("--- Edit category " + category.getId() + " " + category.getName() + " ---");
            System.out.println("1. Edit category details");
            System.out.println("2. Edit candidates");
            System.out.println("0. Back");

            String choice = input.readLine("> ");
            switch (choice) {
                case "1" -> System.out.println("Not built yet.");
                case "2" -> editCandidates(category);
                case "0" -> editing = false;
                default -> System.out.println("Unknown option: " + choice);
            }
        }
    }

    /** Lists the candidates, lets the admin pick one by id and give it a new name, until Enter is pressed. */
    private void editCandidates(Category category) {
        if (category.getBallotType() == BallotType.YES_NO) {
            System.out.println(category.getName() + " is a Yes / No question, so it has no candidates.");
            return;
        }
        while (true) {
            List<Candidate> candidates = category.getCandidates();
            if (candidates.isEmpty()) {
                System.out.println(category.getName() + " has no candidates.");
                return;
            }
            System.out.println();
            System.out.println("Candidates for " + category.getName() + ":");
            int idWidth = candidates.stream().mapToInt(c -> c.getId().length()).max().orElse(0);
            for (Candidate candidate : candidates) {
                System.out.printf("  %-" + idWidth + "s  %s%n", candidate.getId(), candidate.getName());
            }

            String candidateId = input.readLine("Candidate ID to rename (Enter to go back): ");
            if (candidateId.isEmpty()) {
                return;
            }
            Optional<Candidate> selected = category.findCandidate(candidateId);
            if (selected.isEmpty()) {
                System.out.println("There is no candidate " + candidateId + " in " + category.getName());
                continue;
            }
            renameCandidate(category, selected.get());
        }
    }

    private void renameCandidate(Category category, Candidate candidate) {
        while (true) {
            String newName = input.readLine("New name for " + candidate.getName() + " (Enter to keep it): ");
            if (newName.isEmpty()) {
                return;
            }
            if (!NameRules.isValidName(newName)) {
                System.out.println("'" + newName + "' may only contain " + NameRules.ALLOWED_DESCRIPTION + ".");
                continue;
            }
            String oldName = candidate.getName();
            try {
                managementService.renameCandidate(admin, election, category, candidate, newName);
                System.out.println(candidate.getId() + " " + oldName + " is now " + candidate.getName() + ".");
                return;
            } catch (ElectionException e) {
                System.out.println(e.getMessage() + ".");
            }
        }
    }
}
