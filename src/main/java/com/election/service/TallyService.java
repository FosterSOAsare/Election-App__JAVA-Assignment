package com.election.service;

import com.election.exception.ElectionStateException;
import com.election.model.Ballot;
import com.election.model.Category;
import com.election.model.CategoryResult;
import com.election.model.Election;
import com.election.model.ElectionStatus;
import com.election.repository.ElectionRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Results only exist once voting has ended, so everything here works on closed elections. */
public class TallyService {

    private final ElectionRepository electionRepository;
    private final CountingMethodRegistry countingMethods;

    public TallyService(ElectionRepository electionRepository, CountingMethodRegistry countingMethods) {
        this.electionRepository = electionRepository;
        this.countingMethods = countingMethods;
    }

    public List<Election> closedElections() {
        return electionRepository.findByStatus(ElectionStatus.CLOSED);
    }

    /**
     * One result per category, in the election's category order.
     * Each category is counted by the method its ballot type asks for; this class never names one.
     */
    public Map<Category, CategoryResult> count(Election election) throws ElectionStateException {
        if (election.getStatus() != ElectionStatus.CLOSED) {
            throw new ElectionStateException("Results for " + election.getName()
                    + " are only available once it is closed (it is " + election.getStatus() + ")");
        }

        Map<Category, CategoryResult> results = new LinkedHashMap<>();
        for (Category category : election.getCategories()) {
            List<Ballot> ballots = election.getBallots().stream()
                    .filter(b -> b.getCategoryId().equals(category.getId()))
                    .toList();
            CountingMethod method = countingMethods.methodFor(category.getBallotType());
            results.put(category, method.count(category, ballots));
        }
        return results;
    }
}
