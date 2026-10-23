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
import com.election.model.NameRules;
import com.election.model.User;
import com.election.repository.ElectionRepository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

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

    /**
     * The elections this admin created that are in the given status.
     * An admin may only manage their own elections: DRAFT ones to edit, OPEN ones to close.
     */
    public List<Election> findAdminElections(User admin, ElectionStatus status) {
        return electionRepository.findByStatus(status).stream()
                .filter(e -> isCreatedBy(e, admin))
                .toList();
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
        return deadlinePassed(election.getDeadline());
    }

    /** True from the moment the deadline is reached; a new or edited election needs a deadline still to come. */
    public boolean deadlinePassed(LocalDateTime deadline) {
        return !LocalDateTime.now(clock).isBefore(deadline);
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

    /** Only the creator may remove a candidate, and only while the election is still a DRAFT. */
    public void removeCandidate(User admin, Election election, Category category, Candidate candidate)
            throws ElectionException {
        requireCreator(admin, election, "edit");
        requireDraft(election);
        election.removeCandidate(category, candidate);
    }

    /** Only the creator may remove a category (with its candidates), and only while the election is still a DRAFT. */
    public void removeCategory(User admin, Election election, Category category) throws ElectionException {
        requireCreator(admin, election, "edit");
        requireDraft(election);
        election.removeCategory(category);
    }

    /**
     * Deletes a whole draft. Only drafts can be deleted: once an election is open it may have ballots,
     * and those must never disappear.
     */
    public void deleteElection(User admin, Election election) throws ElectionException {
        requireCreator(admin, election, "delete");
        requireDraft(election);
        electionRepository.delete(election.getId());
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
            throws ElectionException {
        // The same rules as editing a draft's details, so a new election can always be opened later.
        if (name == null || name.isBlank() || name.contains(",")) {
            throw new InvalidNameException("Election name '" + name + "' must not be empty or contain a comma");
        }
        if (deadlinePassed(deadline)) {
            throw new ElectionStateException("The deadline " + Deadlines.format(deadline) + " has already passed");
        }
        Election election = new Election(electionRepository.nextId(), name, deadline, ElectionStatus.DRAFT,
                admin.getId());
        addNewCategories(election, categories);
        electionRepository.add(election);
        return election;
    }

    /** Adds categories, with their candidates, to one of the admin's drafts. Ids continue after the existing ones. */
    public void addCategories(User admin, Election election, List<NewCategory> categories) throws ElectionException {
        requireCreator(admin, election, "edit");
        requireDraft(election);
        addNewCategories(election, categories);
    }

    /**
     * Changes a draft's name and/or deadline; null means "keep the current one".
     * The new deadline must not have passed already, or the draft could never be opened.
     * Everything is checked before anything changes, so a refused edit leaves the election as it was.
     */
    public void updateElectionDetails(User admin, Election election, String newName, LocalDateTime newDeadline)
            throws ElectionException {
        requireCreator(admin, election, "edit");
        requireDraft(election);
        if (newDeadline != null && !LocalDateTime.now(clock).isBefore(newDeadline)) {
            throw new ElectionStateException("The new deadline " + Deadlines.format(newDeadline)
                    + " has already passed");
        }
        // rename checks the name before changing anything; after it, changeDeadline cannot fail on a draft.
        if (newName != null) {
            election.rename(newName);
        }
        if (newDeadline != null) {
            election.changeDeadline(newDeadline);
        }
    }

    /**
     * Gives a draft's category another ballot type. Changing to Yes / No removes its candidates;
     * newCandidateNames are added after the change (needed when a Yes / No becomes Select one or multiple)
     * and get the next free Cn ids. Names are checked before anything changes.
     */
    public void changeCategoryType(User admin, Election election, Category category, BallotType newType,
                                   int newSeats, List<String> newCandidateNames) throws ElectionException {
        requireCreator(admin, election, "edit");
        requireDraft(election);
        if (newType == BallotType.YES_NO && !newCandidateNames.isEmpty()) {
            throw new IllegalArgumentException("A Yes / No question cannot have candidates");
        }
        checkNewCandidateNames(category, newCandidateNames);
        election.changeCategoryType(category, newType, newSeats);
        addCandidatesWithNextIds(election, category, newCandidateNames);
    }

    /** Adds candidates to a Select one or Select multiple category of one of the admin's drafts. */
    public void addCandidates(User admin, Election election, Category category, List<String> names)
            throws ElectionException {
        requireCreator(admin, election, "edit");
        requireDraft(election);
        if (category.getBallotType() == BallotType.YES_NO) {
            throw new ElectionStateException(category.getName() + " is a Yes / No question, so it has no candidates");
        }
        checkNewCandidateNames(category, names);
        addCandidatesWithNextIds(election, category, names);
    }

    /** Every name valid, none twice, none already standing in the category. Checked before anything is added. */
    private void checkNewCandidateNames(Category category, List<String> names) throws InvalidNameException {
        for (int i = 0; i < names.size(); i++) {
            String candidateName = names.get(i);
            NameRules.requireValidName(candidateName, "Candidate name");
            boolean standing = category.getCandidates().stream()
                    .anyMatch(c -> c.getName().equalsIgnoreCase(candidateName));
            boolean typedTwice = names.subList(0, i).stream().anyMatch(candidateName::equalsIgnoreCase);
            if (standing || typedTwice) {
                throw new InvalidNameException("Candidate " + candidateName + " is already standing for "
                        + category.getName());
            }
        }
    }

    /** New candidates get the next free Cn in the whole election, so ids never clash across categories. */
    private void addCandidatesWithNextIds(Election election, Category category, List<String> names)
            throws InvalidNameException {
        int candidateNumber = highestNumber("C", election.getCategories().stream()
                .flatMap(c -> c.getCandidates().stream())
                .map(Candidate::getId));
        for (String candidateName : names) {
            candidateNumber++;
            category.addCandidate(new Candidate("C" + candidateNumber, candidateName,
                    election.getId(), category.getId()));
        }
    }

    /** Only the creator may rename a category, and only while the election is still a DRAFT. */
    public void renameCategory(User admin, Election election, Category category, String newName)
            throws ElectionException {
        requireCreator(admin, election, "edit");
        requireDraft(election);
        election.renameCategory(category, newName);
    }

    /**
     * Gives each new category the next free CATn and each new candidate the next free Cn in this election,
     * so adding to an election that already has CAT1-CAT2 and C1-C7 starts at CAT3 and C8.
     * Every name is checked first, so a problem leaves the election exactly as it was.
     */
    private void addNewCategories(Election election, List<NewCategory> categories) throws InvalidNameException {
        for (NewCategory newCategory : categories) {
            boolean nameTaken = election.getCategories().stream()
                    .anyMatch(c -> c.getName().equalsIgnoreCase(newCategory.name()));
            if (nameTaken) {
                throw new InvalidNameException("Category " + newCategory.name() + " already exists in "
                        + election.getName());
            }
            for (String candidateName : newCategory.candidateNames()) {
                NameRules.requireValidName(candidateName, "Candidate name");
            }
        }

        int categoryNumber = highestNumber("CAT", election.getCategories().stream().map(Category::getId));
        int candidateNumber = highestNumber("C", election.getCategories().stream()
                .flatMap(c -> c.getCandidates().stream())
                .map(Candidate::getId));

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
    }

    /** The highest number among ids like CAT3 or C7 with this prefix, or 0 if there are none. */
    private static int highestNumber(String prefix, Stream<String> ids) {
        return ids.filter(id -> id.matches(prefix + "\\d+"))
                .mapToInt(id -> Integer.parseInt(id.substring(prefix.length())))
                .max()
                .orElse(0);
    }
}
