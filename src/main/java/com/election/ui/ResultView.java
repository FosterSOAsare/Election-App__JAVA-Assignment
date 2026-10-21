package com.election.ui;

import com.election.model.Category;
import com.election.model.CategoryResult;
import com.election.model.Election;

import java.util.Map;

/** Prints an election's results. Every result describes itself through summary(), whatever its type. */
public class ResultView {

    public void printResults(Election election, Map<Category, CategoryResult> results) {
        System.out.println();
        System.out.println("Results for " + election.getId() + " " + election.getName()
                + " (" + election.getDate() + ")");

        int idWidth = results.keySet().stream().mapToInt(c -> c.getId().length()).max().orElse(0);
        // The result lines start under the category name, not under its id.
        String resultIndent = "  " + " ".repeat(idWidth + 2);

        for (CategoryResult result : results.values()) {
            Category category = result.getCategory();
            System.out.println();
            System.out.printf("  %-" + idWidth + "s  %s  [%s]  %d %s%n", category.getId(), category.getName(),
                    ElectionView.typeDescription(category), result.getBallotCount(),
                    result.getBallotCount() == 1 ? "ballot" : "ballots");
            result.summary().lines().forEach(line -> System.out.println(resultIndent + line));
        }
    }
}
