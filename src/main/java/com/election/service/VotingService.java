package com.election.service;

import com.election.exception.DuplicateVoteException;
import com.election.exception.ElectionException;
import com.election.exception.ElectionStateException;
import com.election.exception.IneligibleVoterException;
import com.election.exception.InvalidBallotException;
import com.election.model.Ballot;
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
import java.util.Locale;

/**
 * Casting votes. A voter's answers are prepared one category at a time and only stored together at the end,
 * so a voter who stops halfway leaves no ballots behind. Every rule is checked here.
 */
public class VotingService {

    private final ElectionRepository electionRepository;
    // Where "today" comes from. The app passes the system clock; a test can pass a fixed one.
    private final Clock clock;

    public VotingService(ElectionRepository electionRepository, Clock clock) {
        this.electionRepository = electionRepository;
        this.clock = clock;
    }

    /** OPEN elections whose deadline has not passed: the ones a voter can still vote in. */
    public List<Election> openElections() {
        return electionRepository.findByStatus(ElectionStatus.OPEN).stream()
                .filter(e -> !deadlinePassed(e))
                .toList();
    }

    /** The deadline is the moment voting ends, so it has passed from that moment on. */
    public boolean deadlinePassed(Election election) {
        return !LocalDateTime.now(clock).isBefore(election.getDeadline());
    }

    /** A voter submits all their ballots for an election at once, so any ballot means they have voted in it. */
    public boolean hasVoted(User voter, Election election) {
        return election.getBallots().stream()
                .anyMatch(b -> b.getVoterId().equalsIgnoreCase(voter.getId()));
    }

    /**
     * Saves all of a voter's ballots for one election together, or none of them.
     * Every ballot is checked again first, because time has passed since prepareBallot:
     * nothing is stored until all of them pass.
     */
    public void submitBallots(User voter, Election election, List<Ballot> ballots) throws ElectionException {
        List<Ballot> checked = new ArrayList<>();
        for (Ballot ballot : ballots) {
            boolean sameCategoryTwice = checked.stream()
                    .anyMatch(b -> b.getCategoryId().equals(ballot.getCategoryId()));
            if (sameCategoryTwice) {
                throw new DuplicateVoteException(voter.getId() + " has two ballots for " + ballot.getCategoryId());
            }
            checked.add(prepareBallot(voter, election, ballot.getCategoryId(), ballot.getChoices()));
        }
        for (Ballot ballot : checked) {
            election.addBallot(ballot);
        }
    }

    /**
     * Checks one category's answer and builds the ballot, but does not store it. The rules, in order:
     * the user can vote, the election is OPEN, its deadline has not passed, they have not voted in it yet,
     * the category exists, and the choices fit the category's ballot type.
     */
    public Ballot prepareBallot(User voter, Election election, String categoryId, List<String> choices)
            throws ElectionException {
        if (!voter.canVote()) {
            throw new IneligibleVoterException(voter.getId() + " is not allowed to vote");
        }
        if (election.getStatus() != ElectionStatus.OPEN) {
            throw new ElectionStateException(election.getId() + " is not open for voting (it is "
                    + election.getStatus() + ")");
        }
        if (deadlinePassed(election)) {
            throw new ElectionStateException("Voting in " + election.getName() + " ended at "
                    + Deadlines.format(election.getDeadline()));
        }
        if (hasVoted(voter, election)) {
            throw new DuplicateVoteException(voter.getId() + " has already voted in " + election.getName());
        }
        Category category = election.findCategory(categoryId)
                .orElseThrow(() -> new InvalidBallotException("There is no category " + categoryId
                        + " in " + election.getId()));

        // The ballot type belongs to the category, so the category decides what a valid answer looks like.
        List<String> checkedChoices = switch (category.getBallotType()) {
            case SELECT_ONE -> checkSelectOne(category, choices);
            case SELECT_MULTIPLE -> checkSelectMultiple(category, choices);
            case YES_NO -> checkYesNo(choices);
        };

        return new Ballot(voter.getId(), election.getId(), category.getId(), checkedChoices);
    }

    private List<String> checkSelectOne(Category category, List<String> choices) throws InvalidBallotException {
        if (choices.size() != 1) {
            throw new InvalidBallotException("Choose exactly 1 candidate for " + category.getName());
        }
        return List.of(candidateId(category, choices.get(0)));
    }

    private List<String> checkSelectMultiple(Category category, List<String> choices) throws InvalidBallotException {
        if (choices.isEmpty() || choices.size() > category.getSeats()) {
            throw new InvalidBallotException("Choose between 1 and " + category.getSeats() + " candidates for "
                    + category.getName());
        }
        List<String> ids = new ArrayList<>();
        for (String choice : choices) {
            String id = candidateId(category, choice);
            if (ids.contains(id)) {
                throw new InvalidBallotException(id + " was chosen twice");
            }
            ids.add(id);
        }
        return ids;
    }

    private List<String> checkYesNo(List<String> choices) throws InvalidBallotException {
        // Locale.ROOT makes "yes" upper-case to "YES" the same way on every computer.
        String answer = choices.size() == 1 ? choices.get(0).toUpperCase(Locale.ROOT) : "";
        if (!answer.equals("YES") && !answer.equals("NO")) {
            throw new InvalidBallotException("Answer YES or NO");
        }
        return List.of(answer);
    }

    /** Finds the candidate in this category whatever case the id was typed in, so "c1" means C1. */
    private String candidateId(Category category, String typedId) throws InvalidBallotException {
        return category.getCandidates().stream()
                .map(Candidate::getId)
                .filter(id -> id.equalsIgnoreCase(typedId))
                .findFirst()
                .orElseThrow(() -> new InvalidBallotException(typedId + " is not a candidate for "
                        + category.getName()));
    }
}
