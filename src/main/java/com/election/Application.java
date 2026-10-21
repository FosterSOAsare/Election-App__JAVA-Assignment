package com.election;

import com.election.exception.InvalidNameException;
import com.election.loader.BallotCsvLoader;
import com.election.loader.CandidateCsvLoader;
import com.election.loader.CategoryCsvLoader;
import com.election.loader.ElectionCsvLoader;
import com.election.loader.UserCsvLoader;
import com.election.model.User;
import com.election.repository.ElectionRepository;
import com.election.repository.UserRepository;
import com.election.service.ElectionManagementService;
import com.election.service.TallyService;
import com.election.service.VotingService;
import com.election.ui.AdminMenu;
import com.election.ui.ConsoleInput;
import com.election.ui.Menu;
import com.election.ui.VoterMenu;

import java.io.IOException;
import java.nio.file.Path;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.Scanner;

public class Application {

    public static void main(String[] args) {
        // Wiring only: load the CSV files, build the services, log the user in, start their menu.
        Path usersFile = Path.of("src/main/resources/users.csv");
        Path electionsFile = Path.of("src/main/resources/elections.csv");
        Path categoriesFile = Path.of("src/main/resources/categories.csv");
        Path candidatesFile = Path.of("src/main/resources/candidates.csv");
        Path ballotsFile = Path.of("src/main/resources/ballots.csv");

        // Order matters: categories need their elections, and candidates need their categories.
        UserRepository userRepository;
        ElectionRepository electionRepository;
        try {
            userRepository = new UserRepository(new UserCsvLoader().load(usersFile));
            electionRepository = new ElectionRepository(new ElectionCsvLoader().load(electionsFile));
            new CategoryCsvLoader(electionRepository).load(categoriesFile);
            new CandidateCsvLoader(electionRepository).load(candidatesFile);
            new BallotCsvLoader(electionRepository).load(ballotsFile);
        } catch (IOException e) {
            System.out.println("Could not read the data files: " + e.getMessage());
            return;
        } catch (InvalidNameException | IllegalArgumentException | DateTimeParseException e) {
            // A file was readable but one of its rows is wrong: bad name, unknown id, duplicate, bad date or enum.
            System.out.println("Invalid data: " + e.getMessage());
            return;
        }

        ElectionManagementService managementService = new ElectionManagementService(electionRepository);
        TallyService tallyService = new TallyService(electionRepository);
        VotingService votingService = new VotingService(electionRepository);

        System.out.println("Welcome to ELECTION APP");

        // One Scanner for the whole run; every menu reads through the same ConsoleInput.
        Scanner scanner = new Scanner(System.in);
        ConsoleInput input = new ConsoleInput(scanner);

        // Keep asking until the ID matches a loaded user.
        Optional<User> user = Optional.empty();
        while (user.isEmpty()) {
            String userId = input.readLine("Enter your user ID: ");

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
            menu = new AdminMenu(loggedIn, managementService, tallyService, input);
        } else {
            menu = new VoterMenu(loggedIn, votingService, input);
        }
        menu.run();

        scanner.close();
    }
}
