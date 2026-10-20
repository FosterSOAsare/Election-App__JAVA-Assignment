package com.election.service;

import com.election.model.Election;
import com.election.model.ElectionStatus;
import com.election.repository.ElectionRepository;

import java.util.List;
import java.util.Optional;

/** Results only exist once voting has ended, so everything here works on closed elections. */
public class TallyService {

    private final ElectionRepository electionRepository;

    public TallyService(ElectionRepository electionRepository) {
        this.electionRepository = electionRepository;
    }

    public List<Election> closedElections() {
        return electionRepository.findByStatus(ElectionStatus.CLOSED);
    }

    public Optional<Election> findClosedElection(String id) {
        return electionRepository.findById(id)
                .filter(e -> e.getStatus() == ElectionStatus.CLOSED);
    }
}
