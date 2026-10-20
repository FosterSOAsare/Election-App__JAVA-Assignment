package com.election.ui;

import com.election.model.User;

import java.util.Scanner;

public class VoterMenu implements Menu {

    private final User voter;
    private final Scanner scanner;

    public VoterMenu(User voter, Scanner scanner) {
        this.voter = voter;
        this.scanner = scanner;
    }

    @Override
    public void run() {
        boolean running = true;
        while (running) {
            System.out.println();
            System.out.println("--- Voter menu (" + voter.getId() + ") ---");
            System.out.println("1. View open elections");
            System.out.println("2. Vote in an election");
            System.out.println("3. View election results");
            System.out.println("0. Exit");
            System.out.print("> ");

            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1", "2", "3" -> System.out.println("Not built yet.");
                case "0" -> running = false;
                default -> System.out.println("Unknown option: " + choice);
            }
        }
        System.out.println("Goodbye.");
    }
}
