package com.election.loader;

import com.election.model.Deadlines;
import com.election.model.Election;
import com.election.model.ElectionStatus;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Loads elections.csv. */
public class ElectionCsvLoader {

    public List<Election> load(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path);
        List<Election> elections = new ArrayList<>();

        // Start at 1 to skip the header row (id,name,deadline,status,createdBy).
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isBlank()) {
                continue;
            }

            String[] columns = line.split(",");
            if (columns.length != 5) {
                throw new IllegalArgumentException(
                        "elections.csv line " + (i + 1) + ": expected 5 columns but found " + columns.length);
            }

            String id = columns[0].trim();
            String name = columns[1].trim();
            LocalDateTime deadline = Deadlines.parse(columns[2]);
            ElectionStatus status = ElectionStatus.valueOf(columns[3].trim());
            String createdBy = columns[4].trim();

            elections.add(new Election(id, name, deadline, status, createdBy));
        }
        return elections;
    }
}
