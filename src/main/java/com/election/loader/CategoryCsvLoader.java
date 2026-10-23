package com.election.loader;

import com.election.exception.DataLoadException;
import com.election.model.BallotType;
import com.election.model.Category;
import com.election.model.Election;
import com.election.repository.ElectionRepository;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Loads categories.csv and adds each category to the election it belongs to. */
public class CategoryCsvLoader implements DataLoader<Category> {

    private static final int ID = 0;
    private static final int ELECTION_ID = 1;
    private static final int NAME = 2;
    private static final int BALLOT_TYPE = 3;
    private static final int SEATS = 4;

    private final CsvReader csvReader = new CsvReader();
    private final ElectionRepository electionRepository;

    public CategoryCsvLoader(ElectionRepository electionRepository) {
        this.electionRepository = electionRepository;
    }

    @Override
    public List<Category> load(Path path) throws DataLoadException {
        List<Category> categories = new ArrayList<>();
        for (CsvReader.Row row : csvReader.read(path, "id", "electionId", "name", "ballotType", "seats")) {
            Election election = electionRepository.findById(row.get(ELECTION_ID))
                    .orElseThrow(() -> row.error("unknown election " + row.get(ELECTION_ID)));
            BallotType ballotType = row.parseEnum(BallotType.class, BALLOT_TYPE);
            int seats = row.parseInt(SEATS);

            // Category ids only have to be unique inside their election (CAT1 can exist in E1 and E2),
            // so the election itself refuses a repeated id or name; Category refuses bad seats.
            try {
                Category category = new Category(row.get(ID), election.getId(), row.get(NAME), ballotType, seats);
                election.addCategory(category);
                categories.add(category);
            } catch (IllegalArgumentException e) {
                throw row.error(e);
            }
        }
        return categories;
    }
}
