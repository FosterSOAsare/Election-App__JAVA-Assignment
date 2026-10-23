package com.election.loader;

import com.election.exception.DataLoadException;
import com.election.model.Deadlines;
import com.election.model.Election;
import com.election.model.ElectionStatus;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/** Loads elections.csv. */
public class ElectionCsvLoader implements DataLoader<Election> {

    private static final int ID = 0;
    private static final int NAME = 1;
    private static final int DEADLINE = 2;
    private static final int STATUS = 3;
    private static final int CREATED_BY = 4;

    private final CsvReader csvReader = new CsvReader();

    @Override
    public List<Election> load(Path path) throws DataLoadException {
        List<Election> elections = new ArrayList<>();
        for (CsvReader.Row row : csvReader.read(path, "id", "name", "deadline", "status", "createdBy")) {
            LocalDateTime deadline;
            try {
                deadline = Deadlines.parse(row.get(DEADLINE));
            } catch (DateTimeParseException e) {
                throw row.error("'" + row.get(DEADLINE) + "' is not a valid deadline; use " + Deadlines.FORMAT_HINT);
            }
            ElectionStatus status = row.parseEnum(ElectionStatus.class, STATUS);
            try {
                elections.add(new Election(row.get(ID), row.get(NAME), deadline, status, row.get(CREATED_BY)));
            } catch (IllegalArgumentException e) {
                throw row.error(e);
            }
        }
        return elections;
    }
}
