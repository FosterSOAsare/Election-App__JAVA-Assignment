package com.election.ui;

import com.election.model.User;

import java.util.Scanner;

public class AdminMenu implements Menu {

    private final User admin;
    private final Scanner scanner;

    public AdminMenu(User admin, Scanner scanner) {
        this.admin = admin;
        this.scanner = scanner;
    }

    @Override
    public void run() {
        boolean running = true;
        while (running) {
            System.out.println();
            System.out.println("--- Admin menu (" + admin.getId() + ") ---");
            System.out.println("1. Create election");
            System.out.println("2. Manage an election");
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
