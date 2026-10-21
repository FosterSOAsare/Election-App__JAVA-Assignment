package com.election.service;

import com.election.exception.ElectionException;
import com.election.exception.InvalidNameException;
import com.election.exception.UnauthorizedActionException;
import com.election.model.BallotType;
import com.election.model.Candidate;
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
     * The elections this admin created that are in the given status.
     * An admin may only manage their own elections: DRAFT ones to edit, OPEN ones to close.
     */
    public List<Election> findAdminElections(User admin, ElectionStatus status) {
        return electionRepository.findByStatus(status).stream()
                .filter(e -> isCreatedBy(e, admin))
                .toList();
    }

    /** One election this admin created, in the given status; empty if the id is unknown, in another status, or someone else's. */
    public Optional<Election> findAnElectionByAdmin(User admin, ElectionStatus status, String id) {
        return electionRepository.findById(id)
                .filter(e -> e.getStatus() == status)
                .filter(e -> isCreatedBy(e, admin));
    }

    /**
     * Ends voting. Permissions are checked first: the user must be an administrator,
     * and the administrator who created this election. Election.close() then refuses anything not OPEN.
     */
    public void closeElection(User admin, Election election) throws ElectionException {
        requireCreator(admin, election, "close");
        election.close();
    }

    private void requireCreator(User admin, Election election, String action) throws UnauthorizedActionException {
        if (!admin.canManageElections()) {
            throw new UnauthorizedActionException(admin.getId() + " is not an administrator, so cannot "
                    + action + " elections");
        }
        if (!isCreatedBy(election, admin)) {
            throw new UnauthorizedActionException(admin.getId() + " cannot " + action + " " + election.getId()
                    + ": only its creator, " + election.getCreatedBy() + ", can");
        }
    }

    private boolean isCreatedBy(Election election, User admin) {
        return election.getCreatedBy().equalsIgnoreCase(admin.getId());
    }

    /** What the admin typed for one category; ids are given out by createElection. */
    public record NewCategory(String name, BallotType ballotType, int seats, List<String> candidateNames) {
    }

    /**
     * A new election always starts as a DRAFT owned by the admin who created it.
     * Its categories are numbered CAT1, CAT2, ... and its candidates C1, C2, ... inside the election,
     * so election id + category id (or candidate id) is the full key.
     *
     * @param categories the categories in the order the admin entered them
     */
    public Election createElection(User admin, String name, LocalDate date, List<NewCategory> categories)
            throws InvalidNameException {
        Election election = new Election(electionRepository.nextId(), name, date, ElectionStatus.DRAFT, admin.getId());

        int categoryNumber = 0;
        int candidateNumber = 0;
        for (NewCategory newCategory : categories) {
            categoryNumber++;
            Category category = new Category("CAT" + categoryNumber, election.getId(), newCategory.name(),
                    newCategory.ballotType(), newCategory.seats());
            election.addCategory(category);

            for (String candidateName : newCategory.candidateNames()) {
                candidateNumber++;
                category.addCandidate(new Candidate("C" + candidateNumber, candidateName,
                        election.getId(), category.getId()));
            }
        }

        electionRepository.add(election);
        return election;
    }
}
