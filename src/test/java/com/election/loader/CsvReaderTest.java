package com.election.loader;

import com.election.exception.DataLoadException;
import com.election.model.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsvReaderTest {

    @TempDir
    Path folder;

    private final CsvReader reader = new CsvReader();

    @Test
    void rowsAreSplitTrimmedAndKnowTheirFileAndLine() throws Exception {
        Path file = CsvFiles.write(folder, "users.csv", "id,role", " A1 , ADMIN ", "V001,VOTER");

        List<CsvReader.Row> rows = reader.read(file, "id", "role");

        assertEquals(2, rows.size());
        assertEquals(List.of("A1", "ADMIN"), rows.get(0).values());
        assertEquals("users.csv", rows.get(0).fileName());
        assertEquals(2, rows.get(0).lineNumber());
        assertEquals(3, rows.get(1).lineNumber());
    }

    @Test
    void blankLinesAreSkippedButLineNumbersStayTrue() throws Exception {
        Path file = CsvFiles.write(folder, "users.csv", "id,role", "", "A1,ADMIN", "   ", "V001,VOTER");

        List<CsvReader.Row> rows = reader.read(file, "id", "role");

        assertEquals(List.of(3, 5), rows.stream().map(CsvReader.Row::lineNumber).toList());
    }

    @Test
    void aFileWithOnlyTheHeaderHasNoRows() throws Exception {
        Path file = CsvFiles.write(folder, "users.csv", "id,role");
        assertTrue(reader.read(file, "id", "role").isEmpty());
    }

    @Test
    void anInvisibleByteOrderMarkBeforeTheHeaderIsIgnored() throws Exception {
        Path file = CsvFiles.write(folder, "users.csv", "﻿id,role", "A1,ADMIN");
        assertEquals(1, reader.read(file, "id", "role").size());
    }

    @Test
    void anEmptyColumnAtTheEndStillCounts() throws Exception {
        Path file = CsvFiles.write(folder, "users.csv", "id,role", "A1,");

        CsvReader.Row row = reader.read(file, "id", "role").get(0);

        assertEquals(List.of("A1", ""), row.values());
    }

    @Test
    void theWrongHeaderIsRefused() throws Exception {
        Path file = CsvFiles.write(folder, "users.csv", "id,name,role", "A1,Abena,ADMIN");

        DataLoadException e = assertThrows(DataLoadException.class, () -> reader.read(file, "id", "role"));
        assertEquals("users.csv line 1: expected the header id,role but found id,name,role", e.getMessage());
    }

    @Test
    void theWrongNumberOfColumnsIsRefusedWithItsLine() throws Exception {
        Path file = CsvFiles.write(folder, "users.csv", "id,role", "A1,ADMIN", "V001,VOTER,extra");

        DataLoadException e = assertThrows(DataLoadException.class, () -> reader.read(file, "id", "role"));
        assertEquals("users.csv line 3: expected 2 columns but found 3", e.getMessage());
    }

    @Test
    void anEmptyFileIsRefused() throws Exception {
        Path file = folder.resolve("users.csv");
        java.nio.file.Files.writeString(file, "");

        DataLoadException e = assertThrows(DataLoadException.class, () -> reader.read(file, "id", "role"));
        assertTrue(e.getMessage().startsWith("users.csv is empty"));
    }

    @Test
    void aMissingFileSaysWhereItWasLookedFor() {
        Path missing = folder.resolve("ballots.csv");

        DataLoadException e = assertThrows(DataLoadException.class, () -> reader.read(missing, "id"));
        assertTrue(e.getMessage().startsWith("ballots.csv was not found at "));
    }

    @Test
    void numbersAndEnumValuesAreReadOrReportedWithTheirLine() throws Exception {
        Path file = CsvFiles.write(folder, "users.csv", "id,role", "2,VOTER", "two,VOTR");
        List<CsvReader.Row> rows = reader.read(file, "id", "role");

        assertEquals(2, rows.get(0).parseInt(0));
        assertEquals(Role.VOTER, rows.get(0).parseEnum(Role.class, 1));

        DataLoadException notANumber = assertThrows(DataLoadException.class, () -> rows.get(1).parseInt(0));
        assertEquals("users.csv line 3: 'two' is not a whole number", notANumber.getMessage());
        DataLoadException notARole = assertThrows(DataLoadException.class,
                () -> rows.get(1).parseEnum(Role.class, 1));
        assertEquals("users.csv line 3: 'VOTR' is not one of [ADMIN, VOTER]", notARole.getMessage());
    }
}
