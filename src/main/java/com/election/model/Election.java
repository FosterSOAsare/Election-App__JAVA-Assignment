package com.election.model;

import com.election.exception.ElectionException;
import com.election.exception.ElectionStateException;
import com.election.exception.InvalidNameException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class Election {

    private final String id;
    private String name;
    private LocalDateTime deadline;
    private final String createdBy;
    private ElectionStatus status;

    // LinkedHashMap keeps the categories in the order they were added.
    private final Map<String, Category> categoriesById = new LinkedHashMap<>();
    private final List<Ballot> ballots = new ArrayList<>();

    public Election(String id, String name, LocalDateTime deadline, ElectionStatus status, String createdBy) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Election id must not be blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Election name must not be blank");
        }
        this.id = id;
        this.name = name;
        this.deadline = deadline;
        this.status = status;
        this.createdBy = createdBy;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public LocalDateTime getDeadline() {
        return deadline;
    }

    public ElectionStatus getStatus() {
        return status;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    /** Only a DRAFT can be renamed: once voting starts, the setup is frozen. */
    public void rename(String newName) throws ElectionException {
        requireDraft("renamed");
        if (newName == null || newName.isBlank()) {
            throw new InvalidNameException("An election name cannot be empty");
        }
        if (newName.contains(",")) {
            throw new InvalidNameException("Election name '" + newName + "' cannot contain a comma");
        }
        this.name = newName;
    }

    /** Only a DRAFT's deadline can be changed. Whether the new deadline is still in the future is the service's check. */
    public void changeDeadline(LocalDateTime newDeadline) throws ElectionStateException {
        requireDraft("given a new deadline");
        if (newDeadline == null) {
            throw new IllegalArgumentException("A deadline is required");
        }
        this.deadline = newDeadline;
    }

    /** Changes one of this draft's categories to another ballot type; see Category.changeType. */
    public void changeCategoryType(Category category, BallotType newType, int newSeats) throws ElectionStateException {
        requireDraft("edited");
        if (categoriesById.get(category.getId()) != category) {
            throw new IllegalArgumentException("Category " + category.getId() + " is not in " + id);
        }
        category.changeType(newType, newSeats);
    }

    /** Removes one of this draft's categories together with its candidates. Other ids are not renumbered. */
    public void removeCategory(Category category) throws ElectionStateException {
        requireDraft("edited");
        if (categoriesById.get(category.getId()) != category) {
            throw new IllegalArgumentException("Category " + category.getId() + " is not in " + id);
        }
        categoriesById.remove(category.getId());
    }

    /** Removes one candidate from one of this draft's categories. Other ids are not renumbered. */
    public void removeCandidate(Category category, Candidate candidate) throws ElectionStateException {
        requireDraft("edited");
        if (categoriesById.get(category.getId()) != category) {
            throw new IllegalArgumentException("Category " + category.getId() + " is not in " + id);
        }
        category.removeCandidate(candidate);
    }

    private void requireDraft(String action) throws ElectionStateException {
        if (status != ElectionStatus.DRAFT) {
            throw new ElectionStateException(id + " is " + status + ", so it cannot be " + action
                    + "; only DRAFT elections can be edited");
        }
    }

    /** DRAFT to OPEN. The lifecycle only moves forward, so opening anything that is not a DRAFT is refused. */
    public void open() throws ElectionStateException {
        if (status != ElectionStatus.DRAFT) {
            throw new ElectionStateException(id + " cannot be opened because it is " + status
                    + "; only DRAFT elections can be opened");
        }
        status = ElectionStatus.OPEN;
    }

    /** OPEN to CLOSED. The lifecycle only moves forward, so closing anything that is not OPEN is refused. */
    public void close() throws ElectionStateException {
        if (status != ElectionStatus.OPEN) {
            throw new ElectionStateException(id + " cannot be closed because it is " + status
                    + "; only OPEN elections can be closed");
        }
        status = ElectionStatus.CLOSED;
    }

    public List<Category> getCategories() {
        return List.copyOf(categoriesById.values());
    }

    public Optional<Category> findCategory(String categoryId) {
        return Optional.ofNullable(categoriesById.get(categoryId));
    }

    public List<Ballot> getBallots() {
        return List.copyOf(ballots);
    }

    public void addBallot(Ballot ballot) {
        if (!ballot.getElectionId().equals(id)) {
            throw new IllegalArgumentException("Ballot from " + ballot.getVoterId() + " belongs to another election");
        }
        ballots.add(ballot);
    }

    /**
     * Renames one of this election's categories. The name must not be empty, must not contain a comma
     * (category names are typed as a comma-separated list), and must not belong to another category here.
     */
    public void renameCategory(Category category, String newName) throws InvalidNameException {
        if (categoriesById.get(category.getId()) != category) {
            throw new IllegalArgumentException("Category " + category.getId() + " is not in " + id);
        }
        if (newName == null || newName.isBlank()) {
            throw new InvalidNameException("A category name cannot be empty");
        }
        if (newName.contains(",")) {
            throw new InvalidNameException("Category name '" + newName + "' cannot contain a comma");
        }
        boolean nameTaken = categoriesById.values().stream()
                .filter(c -> c != category)
                .anyMatch(c -> c.getName().equalsIgnoreCase(newName));
        if (nameTaken) {
            throw new InvalidNameException("Category " + newName + " already exists in " + name);
        }
        category.rename(newName);
    }

    public void addCategory(Category category) {
        if (!category.getElectionId().equals(id)) {
            throw new IllegalArgumentException("Category " + category.getId() + " belongs to another election");
        }
        if (categoriesById.containsKey(category.getId())) {
            throw new IllegalArgumentException("Duplicate category id " + category.getId() + " in " + id);
        }
        boolean nameTaken = categoriesById.values().stream()
                .anyMatch(c -> c.getName().equalsIgnoreCase(category.getName()));
        if (nameTaken) {
            throw new IllegalArgumentException("Category " + category.getName() + " already exists in " + name);
        }
        categoriesById.put(category.getId(), category);
    }
}
