package com.election.ui;

import com.election.model.BallotType;
import com.election.service.ElectionManagementService.NewCategory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * The questions for new categories, shared by Create Election and Add categories:
 * names separated by commas, then each category's type, seats (multiple only) and candidates (not for yes/no).
 */
public class CategoryForm {

    private final ConsoleInput input;

    public CategoryForm(ConsoleInput input) {
        this.input = input;
    }

    /** For a new election, which has no categories yet. */
    public List<NewCategory> askCategories() {
        return askCategories(List.of());
    }

    /**
     * @param existingNames the election's current category names; a new name that matches one (ignoring case)
     *                      is refused straight away, before any other question is asked
     */
    public List<NewCategory> askCategories(List<String> existingNames) {
        List<String> categoryNames;
        while (true) {
            categoryNames = input.readNames("Categories, separated by commas (e.g. President, Treasurer; 0 to cancel): ",
                    false);
            Optional<String> taken = categoryNames.stream()
                    .filter(n -> existingNames.stream().anyMatch(n::equalsIgnoreCase))
                    .findFirst();
            if (taken.isEmpty()) {
                break;
            }
            System.out.println("There is already a category called " + taken.get() + ". Choose other names.");
        }

        List<NewCategory> categories = new ArrayList<>();
        for (String categoryName : categoryNames) {
            categories.add(askCategoryDetails(categoryName));
        }
        return categories;
    }

    /** Asks for one category's type first, then seats (multiple only), then candidates (not for yes/no). */
    private NewCategory askCategoryDetails(String categoryName) {
        System.out.println();
        List<String> typeLabels = Arrays.stream(BallotType.values()).map(BallotType::getLabel).toList();
        BallotType ballotType = BallotType.values()[input.readChoiceOrCancel("Type for " + categoryName + ":", typeLabels)];

        int seats = ballotType == BallotType.SELECT_MULTIPLE
                ? input.readIntAtLeast("How many winners for " + categoryName + "? (2 or more, 0 to cancel): ", 2)
                : 1;

        List<String> candidateNames = ballotType == BallotType.YES_NO
                ? List.of()
                : askCandidateNames(categoryName, ballotType, seats);

        return new NewCategory(categoryName, ballotType, seats, candidateNames);
    }

    /** A select-multiple category needs more candidates than winners, or there would be nothing to choose. */
    private List<String> askCandidateNames(String categoryName, BallotType ballotType, int seats) {
        while (true) {
            List<String> names = input.readNames("Candidates for " + categoryName + ", separated by commas (0 to cancel): ",
                    true);
            if (ballotType != BallotType.SELECT_MULTIPLE || names.size() > seats) {
                return names;
            }
            System.out.println("With " + seats + " winners you need at least " + (seats + 1) + " candidates.");
        }
    }
}
