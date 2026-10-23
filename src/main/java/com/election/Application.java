package com.election;

import com.election.exception.DataLoadException;
import com.election.loader.BallotCsvLoader;
import com.election.loader.CandidateCsvLoader;
import com.election.loader.CategoryCsvLoader;
import com.election.loader.ElectionCsvLoader;
import com.election.loader.UserCsvLoader;
import com.election.model.User;
import com.election.repository.ElectionRepository;
import com.election.repository.UserRepository;
import com.election.service.BallotFactory;
import com.election.service.CountingMethodRegistry;
import com.election.service.ElectionManagementService;
import com.election.service.TallyService;
import com.election.service.VotingService;
import com.election.ui.ConsoleInput;
import com.election.ui.MenuFactory;

import java.nio.file.Path;
import java.time.Clock;
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

        // One BallotFactory, so seeded ballots and console votes are built and checked the same way.
        BallotFactory ballotFactory = new BallotFactory();

        // Order matters: categories need their elections, candidates their categories, ballots all three.
        UserRepository userRepository;
        ElectionRepository electionRepository;
        try {
            userRepository = new UserRepository(new UserCsvLoader().load(usersFile));
            electionRepository = new ElectionRepository(new ElectionCsvLoader().load(electionsFile));
            new CategoryCsvLoader(electionRepository).load(categoriesFile);
            new CandidateCsvLoader(electionRepository).load(candidatesFile);
            new BallotCsvLoader(electionRepository, ballotFactory).load(ballotsFile);
        } catch (DataLoadException e) {
            // A file is missing or unreadable, or one of its rows is wrong; the message names the file and line.
            System.out.println("Could not load the data: " + e.getMessage());
            return;
        } catch (IllegalArgumentException e) {
            // The repositories refuse a user or election id that appears twice.
            System.out.println("Invalid data: " + e.getMessage());
            return;
        }

        // One clock for every rule that depends on today's date (the election date is the voting deadline).
        Clock clock = Clock.systemDefaultZone();
        ElectionManagementService managementService = new ElectionManagementService(electionRepository, clock);
        TallyService tallyService = new TallyService(electionRepository, new CountingMethodRegistry());
        VotingService votingService = new VotingService(electionRepository, clock, ballotFactory);

        System.out.println("Welcome to ELECTION APP");

        // One Scanner for the whole run; every menu reads through the same ConsoleInput.
        Scanner scanner = new Scanner(System.in);
        ConsoleInput input = new ConsoleInput(scanner);
        MenuFactory menuFactory = new MenuFactory(managementService, votingService, tallyService, input);

        // Log in, use the menu, log out, and repeat until someone types "exit" at the login prompt.
        // The data stays in memory between logins, so one user's votes are there for the next.
        while (true) {
            Optional<User> user = login(userRepository, input);
            if (user.isEmpty()) {
                break;
            }

            User loggedIn = user.get();
            String role = loggedIn.canManageElections() ? "administrator" : "voter";
            System.out.println("Logged in as " + loggedIn.getId() + " (" + role + ")");

            menuFactory.menuFor(loggedIn).run();
        }

        System.out.println("Goodbye.");
        scanner.close();
    }

    /** Keeps asking until the ID matches a loaded user. Empty means the person typed "exit". */
    private static Optional<User> login(UserRepository userRepository, ConsoleInput input) {
        while (true) {
            System.out.println();
            String userId = input.readLine("Enter your user ID (or 'exit' to quit): ");
            if (userId.equalsIgnoreCase("exit")) {
                return Optional.empty();
            }

            Optional<User> user = userRepository.findById(userId);
            if (user.isPresent()) {
                return user;
            }
            System.out.println("Unknown user ID: " + userId + ". Try again.");
        }
    }
}
