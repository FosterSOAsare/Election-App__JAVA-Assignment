package com.election.loader;

import com.election.exception.DataLoadException;
import com.election.exception.InvalidBallotException;
import com.election.model.Ballot;
import com.election.model.Category;
import com.election.model.Election;
import com.election.repository.ElectionRepository;
import com.election.service.BallotFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Loads ballots.csv. Each row goes through BallotFactory, so a seeded ballot is built and checked
 * by exactly the same rules as a vote typed in the console.
 */
public class BallotCsvLoader implements DataLoader<Ballot> {

    private static final int VOTER_ID = 0;
    private static final int ELECTION_ID = 1;
    private static final int CATEGORY_ID = 2;
    private static final int CHOICES = 3;

    // Several choices are separated by | because commas already split the columns.
    private static final Pattern CHOICE_SEPARATOR = Pattern.compile(Pattern.quote("|"));

    private final CsvReader csvReader = new CsvReader();
    private final ElectionRepository electionRepository;
    private final BallotFactory ballotFactory;

    public BallotCsvLoader(ElectionRepository electionRepository, BallotFactory ballotFactory) {
        this.electionRepository = electionRepository;
        this.ballotFactory = ballotFactory;
    }

    @Override
    public List<Ballot> load(Path path) throws DataLoadException {
        List<Ballot> ballots = new ArrayList<>();
        for (CsvReader.Row row : csvReader.read(path, "voterId", "electionId", "categoryId", "choices")) {
            Election election = electionRepository.findById(row.get(ELECTION_ID))
                    .orElseThrow(() -> row.error("unknown election " + row.get(ELECTION_ID)));
            Category category = election.findCategory(row.get(CATEGORY_ID))
                    .orElseThrow(() -> row.error("unknown category " + row.get(CATEGORY_ID) + " in "
                            + election.getId()));
            List<String> choices = Arrays.stream(CHOICE_SEPARATOR.split(row.get(CHOICES)))
                    .map(String::trim)
                    .toList();

            try {
                Ballot ballot = ballotFactory.create(row.get(VOTER_ID), category, choices);
                election.addBallot(ballot);
                ballots.add(ballot);
            } catch (InvalidBallotException | IllegalArgumentException e) {
                throw row.error(e);
            }
        }
        return ballots;
    }
}
