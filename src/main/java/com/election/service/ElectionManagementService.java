package com.election.service;

import com.election.model.Election;
import com.election.model.ElectionStatus;
import com.election.model.User;
import com.election.repository.ElectionRepository;

import java.time.LocalDate;
import java.util.List;

/** Administrator actions on elections. */
public class ElectionManagementService {

    private final ElectionRepository electionRepository;

    public ElectionManagementService(ElectionRepository electionRepository) {
        this.electionRepository = electionRepository;
    }

    public List<Election> allElections() {
        return electionRepository.findAll();
    }

    /** A new election always starts as a DRAFT owned by the admin who created it. */
    public Election createElection(User admin, String name, LocalDate date) {
        Election election = new Election(electionRepository.nextId(), name, date, ElectionStatus.DRAFT, admin.getId());
        electionRepository.add(election);
        return election;
    }
}
