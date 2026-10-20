package com.election.repository;

import com.election.model.User;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class UserRepository {

    // Keys are stored in upper case so lookups ignore case.
    private final Map<String, User> usersById = new HashMap<>();

    public UserRepository(List<User> users) {
        for (User user : users) {
            String key = user.getId().toUpperCase();
            if (usersById.containsKey(key)) {
                throw new IllegalArgumentException("Duplicate user id: " + user.getId());
            }
            usersById.put(key, user);
        }
    }

    public Optional<User> findById(String id) {
        return Optional.ofNullable(usersById.get(id.toUpperCase()));
    }
}
