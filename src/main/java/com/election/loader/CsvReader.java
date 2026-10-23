package com.election.loader;

import com.election.exception.DataLoadException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The part every CSV loader shares: read the file, check the header, skip blank lines,
 * split each line into columns and check how many there are. Each row remembers its file and line,
 * so any problem found later can be reported as "candidates.csv line 12: ...".
 */
public class CsvReader {

    /** One data row of a file; the header is line 1. */
    public record Row(String fileName, int lineNumber, List<String> values) {

        public String get(int column) {
            return values.get(column);
        }

        public int parseInt(int column) throws DataLoadException {
            try {
                return Integer.parseInt(get(column));
            } catch (NumberFormatException e) {
                throw error("'" + get(column) + "' is not a whole number");
            }
        }

        /**
         * Reads a column as one of an enum's values, for example a Role or a BallotType.
         * Generic, so the same method works for every enum.
         */
        public <E extends Enum<E>> E parseEnum(Class<E> type, int column) throws DataLoadException {
            try {
                return Enum.valueOf(type, get(column));
            } catch (IllegalArgumentException e) {
                throw error("'" + get(column) + "' is not one of " + Arrays.toString(type.getEnumConstants()));
            }
        }

        public DataLoadException error(String message) {
            return new DataLoadException(fileName + " line " + lineNumber + ": " + message);
        }

        public DataLoadException error(Exception cause) {
            return new DataLoadException(fileName + " line " + lineNumber + ": " + cause.getMessage(), cause);
        }
    }

    public List<Row> read(Path path, String... expectedHeader) throws DataLoadException {
        String fileName = path.getFileName().toString();
        List<String> lines;
        try {
            lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        } catch (NoSuchFileException e) {
            throw new DataLoadException(fileName + " was not found at " + path.toAbsolutePath(), e);
        } catch (IOException e) {
            throw new DataLoadException("Cannot read " + fileName + ": " + e.getMessage(), e);
        }
        if (lines.isEmpty()) {
            throw new DataLoadException(fileName + " is empty; expected the header " + String.join(",", expectedHeader));
        }

        // Some Windows editors put an invisible byte order mark before the first character.
        List<String> header = split(lines.get(0).replace("﻿", ""));
        if (!header.equals(List.of(expectedHeader))) {
            throw new DataLoadException(fileName + " line 1: expected the header " + String.join(",", expectedHeader)
                    + " but found " + String.join(",", header));
        }

        List<Row> rows = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            if (lines.get(i).isBlank()) {
                continue;
            }
            int lineNumber = i + 1;
            List<String> values = split(lines.get(i));
            if (values.size() != expectedHeader.length) {
                throw new DataLoadException(fileName + " line " + lineNumber + ": expected " + expectedHeader.length
                        + " columns but found " + values.size());
            }
            rows.add(new Row(fileName, lineNumber, values));
        }
        return rows;
    }

    private static List<String> split(String line) {
        // -1 keeps empty columns at the end, so "A1," is 2 columns (one empty) instead of 1.
        return Arrays.stream(line.split(",", -1)).map(String::trim).toList();
    }
}
