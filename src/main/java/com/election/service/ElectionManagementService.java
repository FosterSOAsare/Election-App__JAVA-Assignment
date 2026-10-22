package com.election.service;

import com.election.exception.ElectionException;
import com.election.exception.ElectionStateException;
import com.election.exception.InvalidNameException;
import com.election.exception.UnauthorizedActionException;
import com.election.model.BallotType;
import com.election.model.Candidate;
import com.election.model.Category;
import com.election.model.Deadlines;
import com.election.model.Election;
import com.election.model.ElectionStatus;
import com.election.model.User;
import com.election.repository.ElectionRepository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Administrator actions on elections. */
public class ElectionManagementService {

    private final ElectionRepository electionRepository;
    // Where "today" comes from. The app passes the system clock; a test can pass a fixed one.
    private final Clock clock;

    public ElectionManagementService(ElectionRepository electionRepository, Clock clock) {
        this.electionRepository = electionRepository;
        this.clock = clock;
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
     * Starts voting. Permissions are checked first, then the setup rules, then Election.open()
     * refuses anything that is not a DRAFT. All setup problems are reported together.
     */
    public void openElection(User admin, Election election) throws ElectionException {
        requireCreator(admin, election, "open");

        List<String> problems = new ArrayList<>();
        // There is no point opening an election whose voting deadline has already passed.
        if (deadlinePassed(election)) {
            problems.add("its deadline " + Deadlines.format(election.getDeadline()) + " has already passed");
        }
        if (election.getCategories().isEmpty()) {
            problems.add("it has no categories");
        }
        for (Category category : election.getCategories()) {
            int candidates = category.getCandidates().size();
            if (category.getBallotType() != BallotType.YES_NO && candidates == 0) {
                problems.add(category.getName() + " has no candidates");
            }
            if (category.getBallotType() == BallotType.SELECT_MULTIPLE && candidates > 0
                    && candidates <= category.getSeats()) {
                problems.add(category.getName() + " has " + candidates + " candidates for " + category.getSeats()
                        + " seats; it needs more candidates than seats");
            }
        }
        if (!problems.isEmpty()) {
            throw new ElectionStateException(election.getId() + " is not ready to open: "
                    + String.join("; ", problems));
        }

        election.open();
    }

    /** The deadline is the moment voting ends, so it has passed from that moment on. */
    public boolean deadlinePassed(Election election) {
        return !LocalDateTime.now(clock).isBefore(election.getDeadline());
    }

    /**
     * Ends voting. Closing before the deadline is allowed on purpose so the app can be tested without waiting;
     * a real election would only allow it once deadlinePassed(election) is true.
     * Permissions are checked first: the user must be an administrator,
     * and the administrator who created this election. Election.close() then refuses anything not OPEN.
     */
    public void closeElection(User admin, Election election) throws ElectionException {
        requireCreator(admin, election, "close");
        election.close();
    }

    /** Only the creator may rename a candidate, and only while the election is still a DRAFT. */
    public void renameCandidate(User admin, Election election, Category category, Candidate candidate,
                                String newName) throws ElectionException {
        requireCreator(admin, election, "edit");
        requireDraft(election);
        category.renameCandidate(candidate, newName);
    }

    private void requireDraft(Election election) throws ElectionStateException {
        if (election.getStatus() != ElectionStatus.DRAFT) {
            throw new ElectionStateException(election.getId() + " is " + election.getStatus()
                    + "; only DRAFT elections can be edited");
        }
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
    public Election createElection(User admin, String name, LocalDateTime deadline, List<NewCategory> categories)
            throws InvalidNameException {
        Election election = new Election(electionRepository.nextId(), name, deadline, ElectionStatus.DRAFT,
                admin.getId());

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
