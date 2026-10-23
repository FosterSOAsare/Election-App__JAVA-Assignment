package com.election.loader;

import com.election.exception.DataLoadException;
import com.election.model.Administrator;
import com.election.model.Role;
import com.election.model.User;
import com.election.model.Voter;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Loads users.csv and builds an Administrator or a Voter from the role column. */
public class UserCsvLoader implements DataLoader<User> {

    private static final int ID = 0;
    private static final int ROLE = 1;

    private final CsvReader csvReader = new CsvReader();

    @Override
    public List<User> load(Path path) throws DataLoadException {
        List<User> users = new ArrayList<>();
        for (CsvReader.Row row : csvReader.read(path, "id", "role")) {
            Role role = row.parseEnum(Role.class, ROLE);
            try {
                User user = switch (role) {
                    case ADMIN -> new Administrator(row.get(ID));
                    case VOTER -> new Voter(row.get(ID));
                };
                users.add(user);
            } catch (IllegalArgumentException e) {
                throw row.error(e);
            }
        }
        return users;
    }
}
