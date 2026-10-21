package com.election.loader;

import com.election.model.Category;
import com.election.model.Election;
import com.election.repository.ElectionRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Loads categories.csv and adds each category to the election it belongs to. */
public class CategoryCsvLoader {

    private final ElectionRepository electionRepository;

    public CategoryCsvLoader(ElectionRepository electionRepository) {
        this.electionRepository = electionRepository;
    }

    public List<Category> load(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path);
        List<Category> categories = new ArrayList<>();

        // Start at 1 to skip the header row (id,electionId,name,ballotType,seats).
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isBlank()) {
                continue;
            }

            String where = "categories.csv line " + (i + 1) + ": ";
            String[] columns = line.split(",");
            if (columns.length != 5) {
                throw new IllegalArgumentException(where + "expected 5 columns but found " + columns.length);
            }

            String id = columns[0].trim();
            String electionId = columns[1].trim();
            String name = columns[2].trim();
            // Columns 3 and 4 (ballotType, seats) are not read yet.

            //  get the election this category belongs to, and add the category to it.
            Election election = electionRepository.findById(electionId)
                    .orElseThrow(() -> new IllegalArgumentException(where + "unknown election " + electionId));

            // Category ids only have to be unique inside their election (CAT1 can exist in E1 and E2),
            // so the election itself rejects a repeated id or name.
            Category category = new Category(id, election.getId(), name);
            election.addCategory(category);
            categories.add(category);
        }
        return categories;
    }
}
