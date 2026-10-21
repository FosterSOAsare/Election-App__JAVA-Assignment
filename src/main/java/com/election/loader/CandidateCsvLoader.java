package com.election.loader;

import com.election.model.Candidate;
import com.election.model.Category;
import com.election.model.Election;
import com.election.repository.ElectionRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Loads candidates.csv and adds each candidate to its category, found through its election. */
public class CandidateCsvLoader {

    private final ElectionRepository electionRepository;

    public CandidateCsvLoader(ElectionRepository electionRepository) {
        this.electionRepository = electionRepository;
    }

    public List<Candidate> load(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path);
        List<Candidate> candidates = new ArrayList<>();
        // A candidate id only has to be unique inside its election, so the key is "electionId:candidateId".
        Set<String> seenIds = new HashSet<>();

        // Start at 1 to skip the header row (id,name,electionId,categoryId).
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isBlank()) {
                continue;
            }

            String where = "candidates.csv line " + (i + 1) + ": ";
            String[] columns = line.split(",");
            if (columns.length != 4) {
                throw new IllegalArgumentException(where + "expected 4 columns but found " + columns.length);
            }

            String id = columns[0].trim();
            String name = columns[1].trim();
            String electionId = columns[2].trim();
            String categoryId = columns[3].trim();

            // Find the election first, then the category inside it: electionId + categoryId is the full key.
            Election election = electionRepository.findById(electionId)
                    .orElseThrow(() -> new IllegalArgumentException(where + "unknown election " + electionId));
            Category category = election.findCategory(categoryId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            where + "unknown category " + categoryId + " in " + electionId));

            if (!seenIds.add(election.getId() + ":" + id)) {
                throw new IllegalArgumentException(where + "duplicate candidate id " + id + " in " + electionId);
            }

            Candidate candidate = new Candidate(id, name, election.getId(), category.getId());
            try {
                category.addCandidate(candidate);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(where + e.getMessage());
            }
            candidates.add(candidate);
        }
        return candidates;
    }
}
