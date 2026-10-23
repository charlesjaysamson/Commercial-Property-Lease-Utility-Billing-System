package com.nexus.ui;

import com.nexus.dao.ActivityLogDao;
import com.nexus.dao.UserDao;
import com.nexus.model.Role;
import com.nexus.model.User;

import java.util.Optional;
import java.util.Scanner;

/**
 * Handles everything before a user is "inside" the system:
 * Register Account, Login, and sending each person to the menu of their role.
 */
public class Authentication {

    private static final int MAX_LOGIN_ATTEMPTS = 3;

    private final UserDao userDao;          // the ONE shared UserDao from Main
    private final Scanner scanner;          // the ONE shared Scanner from Main
    private final ActivityLogDao logDao = new ActivityLogDao();

    public Authentication(UserDao userDao, Scanner scanner) {
        this.userDao = userDao;
        this.scanner = scanner;
    }

    public void start() {
        // A brand-new database has no accounts, so nobody could approve anyone.
        // The very first account is therefore created as an ACTIVE Administrator.
        if (userDao.countUsers() == 0) {
            setupFirstAdministrator();
        }

        boolean running = true;
        while (running) {
            InputHelper.clearScreen();
            InputHelper.printHeader("MAIN MENU");
            System.out.println();
            System.out.println("[1] Register Account");
            System.out.println("[2] Login");
            System.out.println("[0] Exit");
            InputHelper.printLine(60, '-');

            switch (InputHelper.readInt(scanner, "Enter choice: ")) {
                case 1 -> {
                    register();
                    InputHelper.pause(scanner);
                }
                case 2 -> login();
                case 0 -> {
                    System.out.println();
                    System.out.println("Thank you for using NEXUS Property Management.");
                    running = false;
                }
                default -> {
                    System.out.println("[!] Invalid menu choice.");
                    InputHelper.pause(scanner);
                }
            }
        }
    }

    private void setupFirstAdministrator() {
        InputHelper.clearScreen();
        InputHelper.printHeader("FIRST-TIME SETUP");
        System.out.println();
        System.out.println("No accounts exist yet. Create the first Administrator account.");
        System.out.println();

        String username = InputHelper.readText(scanner, "Username         : ");
        String password = InputHelper.readPassword(scanner, "Password         : ");

        int id = userDao.registerFirstAdministrator(username, password);
        if (id > 0) {
            logDao.log(id, "CREATE", "First administrator account created: " + username);
            System.out.println();
            System.out.println("[OK] Administrator account created (status: ACTIVE).");
        } else {
            System.out.println("[!] Could not create the account. Please restart the program.");
        }
        InputHelper.pause(scanner);
    }

    // ------------------------------------------------------------------
    // Register: works for ALL roles in model/Role.java
    // ------------------------------------------------------------------
    private void register() {
        System.out.println();
        System.out.println("REGISTER ACCOUNT");
        InputHelper.printLine(20, '-');

        String username = InputHelper.readText(scanner, "Username         : ");
        if (userDao.usernameExists(username)) {
            System.out.println("[!] That username is already taken.");
            return;
        }

        String password = InputHelper.readPassword(scanner, "Password         : ");
        String confirm = InputHelper.readPassword(scanner, "Confirm Password : ");
        if (!password.equals(confirm)) {
            System.out.println("[!] Passwords do not match. Registration cancelled.");
            return;
        }

        System.out.println();
        System.out.println("Select a role:");
        Role[] roles = Role.values();
        for (int i = 0; i < roles.length; i++) {
            System.out.println("[" + (i + 1) + "] " + roles[i].name());
        }
        int choice = InputHelper.chooseNumber(scanner, "Enter role number (0 to cancel): ", roles.length);
        if (choice == 0) {
            System.out.println("[!] Registration cancelled.");
            return;
        }
        Role role = roles[choice - 1];

        int id = userDao.register(username, password, role.name());
        if (id > 0) {
            logDao.log(id, "CREATE", "New account registered: " + username + " (" + role.name() + ")");
            System.out.println();
            System.out.println("[OK] Account created. Status: PENDING (waiting for Administrator approval).");
        } else {
            System.out.println("[!] Registration failed. Please try again.");
        }
    }

    // ------------------------------------------------------------------
    // Login: checks the database, then routes by role
    // ------------------------------------------------------------------
    private void login() {
        System.out.println();
        System.out.println("LOGIN");
        InputHelper.printLine(20, '-');

        for (int attempt = 1; attempt <= MAX_LOGIN_ATTEMPTS; attempt++) {
            String username = InputHelper.readText(scanner, "Username : ");
            String password = InputHelper.readPassword(scanner, "Password : ");

            Optional<User> found = userDao.login(username, password);
            if (found.isPresent()) {
                routeToMenu(found.get());     // returns when the user logs out
                return;
            }

            System.out.println();
            System.out.println("[ERROR] Invalid username or password.");
            int remaining = MAX_LOGIN_ATTEMPTS - attempt;
            if (remaining > 0) {
                System.out.println("Attempts remaining: " + remaining);
                System.out.println();
            }
        }

        System.out.println("[!] Too many failed attempts. Returning to the main menu.");
        InputHelper.pause(scanner);
    }

    // Sends the user straight to the menu that belongs to their role.
    private void routeToMenu(User user) {
        if (user.status().equals("INACTIVE")) {
            System.out.println();
            System.out.println("[!] This account has been deactivated. Please contact the Administrator.");
            InputHelper.pause(scanner);
            return;
        }

        System.out.println();
        System.out.println("[OK] Login successful.");

        // New accounts can log in, but cannot use the system until approved.
        if (!user.isActive()) {
            System.out.println();
            System.out.println("Your account status is " + user.status() + ".");
            System.out.println("An Administrator must approve your account before you can use the system.");
            System.out.println("You will now be logged out.");
            InputHelper.pause(scanner);
            return;
        }

        Role role;
        try {
            role = Role.valueOf(user.roles());
        } catch (IllegalArgumentException e) {
            System.out.println("[!] Unknown role '" + user.roles() + "'. Please contact the Administrator.");
            InputHelper.pause(scanner);
            return;
        }

        switch (role) {
            case ADMINISTRATOR -> new AdminMenu(user, userDao, scanner).run();
            case PROPERTY_MANAGER -> new PropertyManagerMenu(user, scanner).run();
            case BILLING_STAFF -> new BillingStaffMenu(user, scanner).run();
        }
    }
}
