package com.election.repository;

import com.election.model.Election;
import com.election.model.ElectionStatus;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ElectionRepository {

    // LinkedHashMap keeps the elections in file order. Keys are upper case so lookups ignore case.
    private final Map<String, Election> electionsById = new LinkedHashMap<>();

    public ElectionRepository(List<Election> elections) {
        for (Election election : elections) {
            String key = election.getId().toUpperCase();
            if (electionsById.containsKey(key)) {
                throw new IllegalArgumentException("Duplicate election id: " + election.getId());
            }
            electionsById.put(key, election);
        }
    }

    public List<Election> findAll() {
        return List.copyOf(electionsById.values());
    }

    public Optional<Election> findById(String id) {
        return Optional.ofNullable(electionsById.get(id.toUpperCase()));
    }

    public List<Election> findByStatus(ElectionStatus status) {
        return electionsById.values().stream()
                .filter(e -> e.getStatus() == status)
                .toList();
    }
}
