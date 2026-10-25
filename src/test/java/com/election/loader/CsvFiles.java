package com.election.loader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Writes a small CSV file for one test into that test's own temporary folder. */
final class CsvFiles {

    private CsvFiles() {
    }

    static Path write(Path folder, String fileName, String... lines) throws IOException {
        Path file = folder.resolve(fileName);
        Files.writeString(file, String.join("\n", lines) + "\n", StandardCharsets.UTF_8);
        return file;
    }
}
