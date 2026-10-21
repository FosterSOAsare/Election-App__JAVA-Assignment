package com.election.service;

import com.election.exception.InvalidNameException;
import com.election.model.Candidate;
import com.election.model.Category;
import com.election.model.Election;
import com.election.model.ElectionStatus;
import com.election.model.User;
import com.election.repository.ElectionRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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
     * Its categories are numbered CAT1, CAT2, ... and its candidates C1, C2, ... inside the election,
     * so election id + category id (or candidate id) is the full key.
     *
     * @param candidatesByCategory each category name, in order, mapped to its candidate names
     */
    public Election createElection(User admin, String name, LocalDate date,
                                   Map<String, List<String>> candidatesByCategory) throws InvalidNameException {
        Election election = new Election(electionRepository.nextId(), name, date, ElectionStatus.DRAFT, admin.getId());

        int categoryNumber = 0;
        int candidateNumber = 0;
        for (Map.Entry<String, List<String>> entry : candidatesByCategory.entrySet()) {
            categoryNumber++;
            Category category = new Category("CAT" + categoryNumber, election.getId(), entry.getKey());
            election.addCategory(category);

            for (String candidateName : entry.getValue()) {
                candidateNumber++;
                category.addCandidate(new Candidate("C" + candidateNumber, candidateName,
                        election.getId(), category.getId()));
            }
        }

        electionRepository.add(election);
        return election;
    }
}
