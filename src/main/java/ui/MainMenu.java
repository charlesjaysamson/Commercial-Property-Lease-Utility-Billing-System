package ui;

import dao.ActivityLogDao;
import dao.UserDao;
import model.Role;
import model.User;

import java.util.Optional;

/**
 * First screen of the app: Register Account, Login, Exit.
 */
public class MainMenu {

    private static final int MAX_LOGIN_ATTEMPTS = 3;

    private final UserDao userDao = new UserDao();
    private final ActivityLogDao logDao = new ActivityLogDao();

    public void run() {
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

            switch (InputHelper.readInt("Enter choice: ")) {
                case 1 -> {
                    register();
                    InputHelper.pause();
                }
                case 2 -> login();
                case 0 -> {
                    System.out.println();
                    System.out.println("Thank you for using NEXUS Property Management.");
                    running = false;
                }
                default -> {
                    System.out.println("[!] Invalid menu choice.");
                    InputHelper.pause();
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

        String username = InputHelper.readText("Username         : ");
        String password = InputHelper.readPassword("Password         : ");

        int id = userDao.registerFirstAdministrator(username, password);
        if (id > 0) {
            logDao.log(id, "CREATE", "First administrator account created: " + username);
            System.out.println();
            System.out.println("[OK] Administrator account created (status: ACTIVE).");
        } else {
            System.out.println("[!] Could not create the account. Please restart the program.");
        }
        InputHelper.pause();
    }

    private void register() {
        System.out.println();
        System.out.println("REGISTER ACCOUNT");
        InputHelper.printLine(20, '-');

        String username = InputHelper.readText("Username         : ");
        if (userDao.usernameExists(username)) {
            System.out.println("[!] That username is already taken.");
            return;
        }

        String password = InputHelper.readPassword("Password         : ");
        String confirm = InputHelper.readPassword("Confirm Password : ");
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
        int choice = InputHelper.chooseNumber("Enter role number (0 to cancel): ", roles.length);
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

    private void login() {
        System.out.println();
        System.out.println("LOGIN");
        InputHelper.printLine(20, '-');

        for (int attempt = 1; attempt <= MAX_LOGIN_ATTEMPTS; attempt++) {
            String username = InputHelper.readText("Username : ");
            String password = InputHelper.readPassword("Password : ");

            Optional<User> found = userDao.login(username, password);
            if (found.isPresent()) {
                User user = found.get();
                if (user.status().equals("INACTIVE")) {
                    System.out.println();
                    System.out.println("[!] This account has been deactivated. Please contact the Administrator.");
                    InputHelper.pause();
                    return;
                }
                System.out.println();
                System.out.println("[OK] Login successful.");
                new DashboardMenu(user).run();   // returns when the user logs out
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
        InputHelper.pause();
    }
}
