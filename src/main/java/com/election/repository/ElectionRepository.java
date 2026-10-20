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
            add(election);
        }
    }

    public void add(Election election) {
        String key = election.getId().toUpperCase();
        if (electionsById.containsKey(key)) {
            throw new IllegalArgumentException("Duplicate election id: " + election.getId());
        }
        electionsById.put(key, election);
    }

    /** Returns true if an election with this id existed and was removed. */
    public boolean delete(String id) {
        return electionsById.remove(id.toUpperCase()) != null;
    }

    /** The next free id: one more than the highest E-number, so E7 after E6. */
    public String nextId() {
        int highest = electionsById.keySet().stream()
                .filter(key -> key.matches("E\\d+"))
                .mapToInt(key -> Integer.parseInt(key.substring(1)))
                .max()
                .orElse(0);
        return "E" + (highest + 1);
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
