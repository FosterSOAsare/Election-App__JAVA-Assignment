package com.election.model;

import java.time.LocalDate;

public class Election {

    private final String id;
    private final String name;
    private final LocalDate date;
    private final String createdBy;
    private ElectionStatus status;

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
}
