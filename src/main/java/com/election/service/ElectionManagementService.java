package com.election.service;

import com.election.model.Category;
import com.election.model.Election;
import com.election.model.ElectionStatus;
import com.election.model.User;
import com.election.repository.ElectionRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Administrator actions on elections. */
public class ElectionManagementService {

    private final ElectionRepository electionRepository;

    public ElectionManagementService(ElectionRepository electionRepository) {
        this.electionRepository = electionRepository;
    }

    public List<Election> allElections() {
        return electionRepository.findAll();
    }

    public Optional<Election> findElection(String id) {
        return electionRepository.findById(id);
    }

    /**
     * A new election always starts as a DRAFT owned by the admin who created it.
     * Its categories are numbered CAT1, CAT2, ... inside the election; election id + category id is the full key.
     */
    public Election createElection(User admin, String name, LocalDate date, List<String> categoryNames) {
        Election election = new Election(electionRepository.nextId(), name, date, ElectionStatus.DRAFT, admin.getId());
        for (int i = 0; i < categoryNames.size(); i++) {
            String categoryId = "CAT" + (i + 1);
            election.addCategory(new Category(categoryId, election.getId(), categoryNames.get(i)));
        }
        electionRepository.add(election);
        return election;
    }
}
