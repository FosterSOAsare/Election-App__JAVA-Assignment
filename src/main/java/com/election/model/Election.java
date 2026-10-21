package com.election.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class Election {

    private final String id;
    private final String name;
    private final LocalDate date;
    private final String createdBy;
    private ElectionStatus status;

    // LinkedHashMap keeps the categories in the order they were added.
    private final Map<String, Category> categoriesById = new LinkedHashMap<>();
    private final List<Ballot> ballots = new ArrayList<>();

    public Election(String id, String name, LocalDate date, ElectionStatus status, String createdBy) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Election id must not be blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Election name must not be blank");
        }
        this.id = id;
        this.name = name;
        this.date = date;
        this.status = status;
        this.createdBy = createdBy;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public LocalDate getDate() {
        return date;
    }

    public ElectionStatus getStatus() {
        return status;
    }

    public String getCreatedBy() {
        return createdBy;
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
