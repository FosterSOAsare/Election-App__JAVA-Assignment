package com.election.loader;

import com.election.model.Administrator;
import com.election.model.Role;
import com.election.model.User;
import com.election.model.Voter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Loads users.csv and builds an Administrator or a Voter from the role column. */
public class UserCsvLoader {

    public List<User> load(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path);
        List<User> users = new ArrayList<>();

        // Start at 1 to skip the header row (id,role).
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isBlank()) {
                continue;
            }

            String[] columns = line.split(",");
            if (columns.length != 2) {
                throw new IllegalArgumentException(
                        "users.csv line " + (i + 1) + ": expected 2 columns but found " + columns.length);
            }

            String id = columns[0].trim();
            Role role = Role.valueOf(columns[1].trim());

            User user = switch (role) {
                case ADMIN -> new Administrator(id);
                case VOTER -> new Voter(id);
            };
            users.add(user);
        }
        return users;
    }
}
