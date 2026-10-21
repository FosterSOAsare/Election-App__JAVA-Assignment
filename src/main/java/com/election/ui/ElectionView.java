package com.election.ui;

import com.election.model.Ballot;
import com.election.model.BallotType;
import com.election.model.Candidate;
import com.election.model.Category;
import com.election.model.Election;

import java.util.List;
import java.util.Map;

/** Prints elections, their categories and their candidates. Reads nothing and changes nothing. */
public class ElectionView {

    /** A table of elections with a header row; each column is padded to its longest value. */
    public void printElectionTable(String title, List<Election> elections) {
        int idWidth = Math.max("ID".length(), elections.stream().mapToInt(e -> e.getId().length()).max().orElse(0));
        int nameWidth = Math.max("Name".length(),
                elections.stream().mapToInt(e -> e.getName().length()).max().orElse(0));
        String row = "  %-" + idWidth + "s  %-" + nameWidth + "s  %-10s  %-6s  %s%n";

        System.out.println(title);
        System.out.printf(row, "ID", "Name", "Date", "Status", "Created by");
        for (Election election : elections) {
            System.out.printf(row, election.getId(), election.getName(), election.getDate(),
                    election.getStatus(), election.getCreatedBy());
        }
    }

    public void printElectionCategories(Election election) {
        if (election.getCategories().isEmpty()) {
            System.out.println(election.getName() + " has no categories yet.");
            return;
        }
        System.out.println();
        System.out.println("Categories in " + election.getId() + " " + election.getName() + ":");
        printCategories(election, "  ");
    }

    /** Every ballot in the election as a table, in file order, for example "V001  CAT2  C4, C5". */
    public void printBallots(Election election) {
        List<Ballot> ballots = election.getBallots();
        System.out.println();
        if (ballots.isEmpty()) {
            System.out.println("No ballots have been cast in " + election.getId() + " yet.");
            return;
        }

        int voterWidth = Math.max("Voter".length(),
                ballots.stream().mapToInt(b -> b.getVoterId().length()).max().orElse(0));
        int categoryWidth = Math.max("Category".length(),
                ballots.stream().mapToInt(b -> b.getCategoryId().length()).max().orElse(0));
        String row = "  %-" + voterWidth + "s  %-" + categoryWidth + "s  %s%n";

        System.out.println("Ballots in " + election.getId() + " (" + ballots.size() + "):");
        System.out.printf(row, "Voter", "Category", "Choices");
        for (Ballot ballot : ballots) {
            System.out.printf(row, ballot.getVoterId(), ballot.getCategoryId(), String.join(", ", ballot.getChoices()));
        }
    }

    /** One category as the voter sees it: its name and type, then the candidates to choose from. */
    public void printBallotPaper(Category category) {
        System.out.println(category.getName() + "  [" + typeDescription(category) + "]");
        int idWidth = category.getCandidates().stream().mapToInt(c -> c.getId().length()).max().orElse(0);
        for (Candidate candidate : category.getCandidates()) {
            System.out.printf("  %-" + idWidth + "s  %s%n", candidate.getId(), candidate.getName());
        }
    }

    /**
     * What the voter chose in every category of the election, before they submit.
     * Candidate ids are shown with their names; a category without an answer shows "(skipped)".
     *
     * @param answers category id to the prepared ballot for it
     */
    public void printVoteSummary(Election election, Map<String, Ballot> answers) {
        int nameWidth = election.getCategories().stream().mapToInt(c -> c.getName().length()).max().orElse(0);

        System.out.println();
        System.out.println("Your votes in " + election.getId() + " " + election.getName() + ":");
        for (Category category : election.getCategories()) {
            Ballot ballot = answers.get(category.getId());
            String shown = ballot == null ? "(skipped)" : describeChoices(category, ballot);
            System.out.printf("  %-" + nameWidth + "s  %s%n", category.getName(), shown);
        }
    }

    private String describeChoices(Category category, Ballot ballot) {
        List<String> shown = ballot.getChoices().stream()
                .map(choice -> category.getCandidates().stream()
                        .filter(c -> c.getId().equals(choice))
                        .map(c -> c.getId() + " " + c.getName())
                        .findFirst()
                        .orElse(choice))   // YES / NO are not candidates, so they are shown as they are
                .toList();
        return String.join(", ", shown);
    }

    public void printCreatedElection(Election election) {
        System.out.println("Created election:");
        System.out.println("  ID:         " + election.getId());
        System.out.println("  Name:       " + election.getName());
        System.out.println("  Date:       " + election.getDate());
        System.out.println("  Status:     " + election.getStatus());
        System.out.println("  Created by: " + election.getCreatedBy());
        System.out.println("  Categories:");
        printCategories(election, "    ");
    }

    /**
     * Prints each category with its type, and its candidates indented underneath, for example:
     *   CAT1  President  [Select one]
     *         C1  Amara Mensah
     *         C2  Kofi Boateng
     * Id widths are taken from the longest id in the whole election, so every category lines up.
     */
    private void printCategories(Election election, String indent) {
        List<Category> categories = election.getCategories();

        int categoryIdWidth = categories.stream().mapToInt(c -> c.getId().length()).max().orElse(0);
        int candidateIdWidth = categories.stream()
                .flatMap(c -> c.getCandidates().stream())
                .mapToInt(c -> c.getId().length())
                .max()
                .orElse(0);

        // Candidates start under the category name, not under its id.
        String candidateIndent = indent + " ".repeat(categoryIdWidth + 2);
        String categoryRow = indent + "%-" + categoryIdWidth + "s  %s  [%s]%n";
        String candidateRow = candidateIndent + "%-" + candidateIdWidth + "s  %s%n";

        for (Category category : categories) {
            System.out.printf(categoryRow, category.getId(), category.getName(), typeDescription(category));

            if (category.getBallotType() == BallotType.YES_NO) {
                System.out.println(candidateIndent + "(voters answer yes or no)");
            } else if (category.getCandidates().isEmpty()) {
                System.out.println(candidateIndent + "(no candidates)");
            }
            for (Candidate candidate : category.getCandidates()) {
                System.out.printf(candidateRow, candidate.getId(), candidate.getName());
            }
        }
    }

    private String typeDescription(Category category) {
        String type = category.getBallotType().getLabel();
        if (category.getBallotType() == BallotType.SELECT_MULTIPLE) {
            type += ", " + category.getSeats() + " winners";
        }
        return type;
    }
}
