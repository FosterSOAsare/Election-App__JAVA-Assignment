package com.election;

import com.election.loader.UserCsvLoader;
import com.election.model.User;
import com.election.ui.AdminMenu;
import com.election.ui.Menu;
import com.election.ui.VoterMenu;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

public class Application {

    public static void main(String[] args) {
        // Wiring only: load the CSV files, build the services, log the user in, start their menu.
        List<User> users;
        try {
            users = new UserCsvLoader().load(Path.of("src/main/resources/users.csv"));
        } catch (IOException e) {
            System.out.println("Could not read users.csv: " + e.getMessage());
            return;
        }

        System.out.println("Welcome to ELECTION APP");

        Scanner scanner = new Scanner(System.in);

        // Keep asking until the ID matches a loaded user.
        Optional<User> user = Optional.empty();
        while (user.isEmpty()) {
            System.out.print("Enter your user ID: ");
            String userId = scanner.nextLine().trim();

            user = users.stream()
                    .filter(u -> u.getId().equalsIgnoreCase(userId))
                    .findFirst();

            if (user.isEmpty()) {
                System.out.println("Unknown user ID: " + userId + ". Try again.");
            }
        }

        User loggedIn = user.get();
        String role = loggedIn.canManageElections() ? "administrator" : "voter";
        System.out.println("Logged in as " + loggedIn.getId() + " (" + role + ")");

        Menu menu;
        if (loggedIn.canManageElections()) {
            menu = new AdminMenu(loggedIn, scanner);
        } else {
            menu = new VoterMenu(loggedIn, scanner);
        }
        menu.run();

        scanner.close();
    }
}
