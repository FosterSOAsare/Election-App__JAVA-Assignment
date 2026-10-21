package com.election.loader;

import com.election.model.Ballot;
import com.election.model.Election;
import com.election.repository.ElectionRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Loads ballots.csv and adds each ballot to its election, as written; the voting rules are not checked yet. */
public class BallotCsvLoader {

    private final ElectionRepository electionRepository;

    public BallotCsvLoader(ElectionRepository electionRepository) {
        this.electionRepository = electionRepository;
    }

    public List<Ballot> load(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path);
        List<Ballot> ballots = new ArrayList<>();

        // Start at 1 to skip the header row (voterId,electionId,categoryId,choices).
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isBlank()) {
                continue;
            }

            String where = "ballots.csv line " + (i + 1) + ": ";
            String[] columns = line.split(",");
            if (columns.length != 4) {
                throw new IllegalArgumentException(where + "expected 4 columns but found " + columns.length);
            }

            String voterId = columns[0].trim();
            String electionId = columns[1].trim();
            String categoryId = columns[2].trim();
            // Several choices are separated by | because commas already split the columns. "\\|" escapes it.
            List<String> choices = Arrays.stream(columns[3].split("\\|"))
                    .map(String::trim)
                    .toList();

            Election election = electionRepository.findById(electionId)
                    .orElseThrow(() -> new IllegalArgumentException(where + "unknown election " + electionId));

            Ballot ballot = new Ballot(voterId, election.getId(), categoryId, choices);
            election.addBallot(ballot);
            ballots.add(ballot);
        }
        return ballots;
    }
}
