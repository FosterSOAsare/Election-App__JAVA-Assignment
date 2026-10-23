package com.election.ui;

import com.election.exception.ElectionException;
import com.election.model.BallotType;
import com.election.model.Candidate;
import com.election.model.Category;
import com.election.model.Deadlines;
import com.election.model.Election;
import com.election.model.NameRules;
import com.election.model.User;
import com.election.service.ElectionManagementService;
import com.election.service.ElectionManagementService.NewCategory;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Editing one draft election: its own details, its categories (and each category's candidates),
 * and adding new categories. Only drafts get here, so nothing can change once voting has started.
 */
public class EditDraftMenu implements Menu {

    private final User admin;
    private final Election election;
    private final ElectionManagementService managementService;
    private final ConsoleInput input;
    private final ElectionView view = new ElectionView();

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
                case "1" -> editElectionDetails();
                case "2" -> pickCategoryToEdit();
                case "3" -> addCategories();
                case "0" -> editing = false;
                default -> System.out.println("Unknown option: " + choice);
            }
        }
    }

    /** Name and deadline; Enter keeps either one. A refused change is asked again, and nothing is half-saved. */
    private void editElectionDetails() {
        while (true) {
            System.out.println();
            System.out.println("Election details of " + election.getId() + ":");
            System.out.println("  Name:      " + election.getName());
            System.out.println("  Deadline:  " + Deadlines.format(election.getDeadline()));

            String newName = input.readLine("New name (Enter to keep it): ");
            Optional<LocalDateTime> newDeadline = input.readOptionalDeadline(
                    "New deadline (" + Deadlines.FORMAT_HINT + "; Enter to keep it): ");

            if (newName.isEmpty() && newDeadline.isEmpty()) {
                System.out.println("Nothing changed.");
                return;
            }
            try {
                managementService.updateElectionDetails(admin, election,
                        newName.isEmpty() ? null : newName, newDeadline.orElse(null));
                System.out.println("Saved: " + election.getId() + " " + election.getName()
                        + ", deadline " + Deadlines.format(election.getDeadline()) + ".");
                return;
            } catch (ElectionException e) {
                System.out.println("Not saved: " + e.getMessage() + ". Try again, or press Enter twice to keep both.");
            }
        }
    }

    /** The same questions as Create Election; the new categories and their candidates are shown afterwards. */
    private void addCategories() {
        List<String> existingNames = election.getCategories().stream().map(Category::getName).toList();
        List<NewCategory> categories = new CategoryForm(input).askCategories(existingNames);
        try {
            managementService.addCategories(admin, election, categories);
            System.out.println("Added " + categories.size() + (categories.size() == 1 ? " category" : " categories")
                    + " to " + election.getId() + ".");
            view.printElectionCategories(election);
        } catch (ElectionException e) {
            System.out.println("Nothing was added: " + e.getMessage() + ".");
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
            System.out.println("3. Add candidates");
            System.out.println("4. Remove this category");
            System.out.println("0. Back");

            String choice = input.readLine("> ");
            switch (choice) {
                case "1" -> editCategoryDetails(category);
                case "2" -> editCandidates(category);
                case "3" -> addCandidates(category);
                // Once it is removed there is nothing left to edit, so this menu ends.
                case "4" -> editing = !removeCategory(category);
                case "0" -> editing = false;
                default -> System.out.println("Unknown option: " + choice);
            }
        }
    }

    /** Returns true if the category was removed. */
    private boolean removeCategory(Category category) {
        String question = "Remove " + category.getId() + " \"" + category.getName() + "\"";
        List<Candidate> candidates = category.getCandidates();
        if (!candidates.isEmpty()) {
            question += " and its " + candidates.size() + (candidates.size() == 1 ? " candidate" : " candidates")
                    + " (" + candidates.stream().map(c -> c.getId() + " " + c.getName())
                    .collect(Collectors.joining(", ")) + ")";
        }
        if (!input.confirm(question + "?")) {
            System.out.println(category.getId() + " was not removed.");
            return false;
        }
        try {
            managementService.removeCategory(admin, election, category);
            System.out.println("Removed " + category.getId() + " \"" + category.getName() + "\".");
            return true;
        } catch (ElectionException e) {
            System.out.println("Could not remove " + category.getId() + ": " + e.getMessage() + ".");
            return false;
        }
    }

    /** Category details: its name, then optionally its type. */
    private void editCategoryDetails(Category category) {
        System.out.println();
        System.out.println("Category details of " + category.getId() + ":");
        System.out.println("  Name:  " + category.getName());
        System.out.println("  Type:  " + ElectionView.typeDescription(category));
        renameCategory(category);

        String answer = input.readLine("Change the type? (yes/no): ");
        if (answer.equalsIgnoreCase("yes") || answer.equalsIgnoreCase("y")) {
            changeCategoryType(category);
        }
    }

    /**
     * Asks for the new type, then whatever that type needs: seats for Select multiple, candidates when there
     * would not be enough, or confirmation before a Yes / No question drops the candidates it has.
     */
    private void changeCategoryType(Category category) {
        List<String> typeLabels = Arrays.stream(BallotType.values()).map(BallotType::getLabel).toList();
        BallotType newType = BallotType.values()[input.readChoice("New type for " + category.getName() + ":",
                typeLabels)];
        int newSeats = newType == BallotType.SELECT_MULTIPLE
                ? input.readIntAtLeast("How many winners for " + category.getName() + "? (2 or more): ", 2)
                : 1;

        if (newType == category.getBallotType() && newSeats == category.getSeats()) {
            System.out.println("That is already its type; nothing changed.");
            return;
        }

        List<String> newCandidateNames = List.of();
        if (newType == BallotType.YES_NO) {
            List<Candidate> candidates = category.getCandidates();
            if (!candidates.isEmpty()) {
                String names = candidates.stream().map(c -> c.getId() + " " + c.getName())
                        .collect(Collectors.joining(", "));
                System.out.println("A Yes / No question has no candidates, so this removes " + names + ".");
                String confirm = input.readLine("Continue? (yes/no): ");
                if (!confirm.equalsIgnoreCase("yes") && !confirm.equalsIgnoreCase("y")) {
                    System.out.println("The type was not changed.");
                    return;
                }
            }
        } else {
            // Select one needs at least 1 candidate, Select multiple more candidates than seats.
            int needed = (newType == BallotType.SELECT_MULTIPLE ? newSeats + 1 : 1) - category.getCandidates().size();
            if (needed > 0) {
                newCandidateNames = askNewCandidateNames(category, needed);
            }
        }

        try {
            managementService.changeCategoryType(admin, election, category, newType, newSeats, newCandidateNames);
            System.out.println(category.getId() + " " + category.getName() + " is now "
                    + ElectionView.typeDescription(category) + ".");
        } catch (ElectionException e) {
            System.out.println("The type was not changed: " + e.getMessage() + ".");
        }
    }

    /** At least `needed` new names, none already standing in this category. */
    private List<String> askNewCandidateNames(Category category, int needed) {
        while (true) {
            System.out.println("It needs at least " + needed + " more " + (needed == 1 ? "candidate" : "candidates") + ".");
            List<String> names = readNamesNotStandingIn(category);
            if (names.size() >= needed) {
                return names;
            }
        }
    }

    /** A comma-separated list of valid names, asked again until none of them is already standing in the category. */
    private List<String> readNamesNotStandingIn(Category category) {
        while (true) {
            List<String> names = input.readNames("Candidates for " + category.getName() + ", separated by commas: ",
                    true);
            Optional<String> taken = names.stream()
                    .filter(n -> category.getCandidates().stream().anyMatch(c -> c.getName().equalsIgnoreCase(n)))
                    .findFirst();
            if (taken.isEmpty()) {
                return names;
            }
            System.out.println(taken.get() + " is already standing for " + category.getName() + ".");
        }
    }

    /** New candidates for a Select one or Select multiple category, shown with their new ids afterwards. */
    private void addCandidates(Category category) {
        if (category.getBallotType() == BallotType.YES_NO) {
            System.out.println(category.getName() + " is a Yes / No question, so it has no candidates.");
            return;
        }
        List<String> names = readNamesNotStandingIn(category);
        try {
            managementService.addCandidates(admin, election, category, names);
            System.out.println("Added " + names.size() + (names.size() == 1 ? " candidate" : " candidates")
                    + " to " + category.getName() + ":");
            for (Candidate candidate : category.getCandidates()) {
                System.out.println("  " + candidate.getId() + "  " + candidate.getName());
            }
        } catch (ElectionException e) {
            System.out.println("Nothing was added: " + e.getMessage() + ".");
        }
    }

    /** Enter keeps the current name; an invalid name is asked again. */
    private void renameCategory(Category category) {
        while (true) {
            String newName = input.readLine("New name for " + category.getName() + " (Enter to keep it): ");
            if (newName.isEmpty()) {
                return;
            }
            String oldName = category.getName();
            try {
                managementService.renameCategory(admin, election, category, newName);
                System.out.println(category.getId() + " " + oldName + " is now " + category.getName() + ".");
                return;
            } catch (ElectionException e) {
                System.out.println(e.getMessage() + ".");
            }
        }
    }

    /** Lists the candidates, lets the admin pick one by id and rename or remove it, until Enter is pressed. */
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

            String candidateId = input.readLine("Candidate ID to edit (Enter to go back): ");
            if (candidateId.isEmpty()) {
                return;
            }
            Optional<Candidate> selected = category.findCandidate(candidateId);
            if (selected.isEmpty()) {
                System.out.println("There is no candidate " + candidateId + " in " + category.getName());
                continue;
            }
            editCandidate(category, selected.get());
        }
    }

    private void editCandidate(Category category, Candidate candidate) {
        System.out.println();
        System.out.println("--- Candidate " + candidate.getId() + " " + candidate.getName() + " ---");
        System.out.println("1. Rename");
        System.out.println("2. Remove");
        System.out.println("0. Back");

        String choice = input.readLine("> ");
        switch (choice) {
            case "1" -> renameCandidate(category, candidate);
            case "2" -> removeCandidate(category, candidate);
            case "0" -> { }
            default -> System.out.println("Unknown option: " + choice);
        }
    }

    private void removeCandidate(Category category, Candidate candidate) {
        if (!input.confirm("Remove " + candidate.getId() + " " + candidate.getName() + " from " + category.getName() + "?")) {
            System.out.println(candidate.getId() + " was not removed.");
            return;
        }
        try {
            managementService.removeCandidate(admin, election, category, candidate);
            System.out.println("Removed " + candidate.getId() + " " + candidate.getName() + ".");
        } catch (ElectionException e) {
            System.out.println("Could not remove " + candidate.getId() + ": " + e.getMessage() + ".");
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
