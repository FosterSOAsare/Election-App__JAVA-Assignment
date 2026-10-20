package com.election;

import com.election.loader.ElectionCsvLoader;
import com.election.loader.UserCsvLoader;
import com.election.model.Election;
import com.election.model.User;
import com.election.repository.ElectionRepository;
import com.election.repository.UserRepository;
import com.election.service.ElectionManagementService;
import com.election.service.TallyService;
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
        Path usersFile = Path.of("src/main/resources/users.csv");
        Path electionsFile = Path.of("src/main/resources/elections.csv");

        List<User> users;
        List<Election> elections;
        try {
            users = new UserCsvLoader().load(usersFile);
            elections = new ElectionCsvLoader().load(electionsFile);
        } catch (IOException e) {
            System.out.println("Could not read the data files: " + e.getMessage());
            return;
        }

        UserRepository userRepository = new UserRepository(users);
        ElectionRepository electionRepository = new ElectionRepository(elections);
        ElectionManagementService managementService = new ElectionManagementService(electionRepository);
        TallyService tallyService = new TallyService(electionRepository);

        System.out.println("Welcome to ELECTION APP");

        Scanner scanner = new Scanner(System.in);

        // Keep asking until the ID matches a loaded user.
        Optional<User> user = Optional.empty();
        while (user.isEmpty()) {
            System.out.print("Enter your user ID: ");
            String userId = scanner.nextLine().trim();

            user = userRepository.findById(userId);
            if (user.isEmpty()) {
                System.out.println("Unknown user ID: " + userId + ". Try again.");
            }
        }

        User loggedIn = user.get();
        String role = loggedIn.canManageElections() ? "administrator" : "voter";
        System.out.println("Logged in as " + loggedIn.getId() + " (" + role + ")");

        Menu menu;
        if (loggedIn.canManageElections()) {
            menu = new AdminMenu(loggedIn, managementService, tallyService, scanner);
        } else {
            menu = new VoterMenu(loggedIn, scanner);
        }
        menu.run();

        scanner.close();
    }
}
