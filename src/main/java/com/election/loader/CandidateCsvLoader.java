package com.election.loader;

import com.election.exception.DataLoadException;
import com.election.exception.InvalidNameException;
import com.election.model.Candidate;
import com.election.model.Category;
import com.election.model.Election;
import com.election.repository.ElectionRepository;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Loads candidates.csv and adds each candidate to its category, found through its election. */
public class CandidateCsvLoader implements DataLoader<Candidate> {

    private static final int ID = 0;
    private static final int NAME = 1;
    private static final int ELECTION_ID = 2;
    private static final int CATEGORY_ID = 3;

    private final CsvReader csvReader = new CsvReader();
    private final ElectionRepository electionRepository;

    public CandidateCsvLoader(ElectionRepository electionRepository) {
        this.electionRepository = electionRepository;
    }

    @Override
    public List<Candidate> load(Path path) throws DataLoadException {
        List<Candidate> candidates = new ArrayList<>();
        // A candidate id only has to be unique inside its election, so the key is "electionId:candidateId".
        Set<String> seenIds = new HashSet<>();

        for (CsvReader.Row row : csvReader.read(path, "id", "name", "electionId", "categoryId")) {
            // Find the election first, then the category inside it: electionId + categoryId is the full key.
            Election election = electionRepository.findById(row.get(ELECTION_ID))
                    .orElseThrow(() -> row.error("unknown election " + row.get(ELECTION_ID)));
            Category category = election.findCategory(row.get(CATEGORY_ID))
                    .orElseThrow(() -> row.error("unknown category " + row.get(CATEGORY_ID) + " in "
                            + election.getId()));
            if (!seenIds.add(election.getId() + ":" + row.get(ID))) {
                throw row.error("duplicate candidate id " + row.get(ID) + " in " + election.getId());
            }

            try {
                Candidate candidate = new Candidate(row.get(ID), row.get(NAME), election.getId(), category.getId());
                category.addCandidate(candidate);
                candidates.add(candidate);
            } catch (InvalidNameException | IllegalArgumentException e) {
                throw row.error(e);
            }
        }
        return candidates;
    }
}
