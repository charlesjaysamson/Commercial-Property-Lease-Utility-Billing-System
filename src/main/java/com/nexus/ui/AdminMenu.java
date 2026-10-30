package com.nexus.ui;

import com.nexus.dao.ActivityLogDao;
import com.nexus.dao.BillingDao;
import com.nexus.dao.PropertyDao;
import com.nexus.dao.UserDao;
import com.nexus.model.Role;
import com.nexus.model.User;

import java.util.List;
import java.util.Scanner;

/**
 * Menu for the ADMINISTRATOR role: manages all system records.
 */
public class AdminMenu {

    private final User user;
    private final UserDao userDao;      // the ONE shared UserDao from Main
    private final Scanner scanner;      // the ONE shared Scanner from Main
    private final PropertyDao propertyDao = new PropertyDao();
    private final BillingDao billingDao = new BillingDao();
    private final ActivityLogDao logDao = new ActivityLogDao();

    public AdminMenu(User user, UserDao userDao, Scanner scanner) {
        this.user = user;
        this.userDao = userDao;
        this.scanner = scanner;
    }

    public void run() {
        boolean loggedIn = true;
        while (loggedIn) {
            // Any bill past its due date becomes OVERDUE and gets its late fee.
            billingDao.markOverdueBills();

            InputHelper.clearScreen();
            InputHelper.printHeader("ADMINISTRATOR DASHBOARD");
            DashboardView.printWelcome(user);
            DashboardView.printPropertyOverview(propertyDao);
            DashboardView.printBillingOverview(billingDao);

            InputHelper.printLine(60, '-');
            System.out.println("[1] Property Management");
            System.out.println("[2] Tenant Management");
            System.out.println("[3] Lease Management");
            System.out.println("[4] Billing & Payments");
            System.out.println("[5] Reports");
            System.out.println("[6] User Accounts");
            System.out.println("[7] Activity Logs");
            System.out.println("[8] Update User Role & Status");
            System.out.println("[0] Logout");
            InputHelper.printLine(60, '-');

            switch (InputHelper.readInt(scanner, "Enter choice: ")) {
                case 1 -> new PropertyMenu(user, scanner).run();
                case 2 -> new TenantMenu(user, scanner).run();
                case 3 -> new LeaseMenu(user, scanner).run();
                case 4 -> new BillingMenu(user, scanner).run();
                case 5 -> new ReportMenu(scanner).run();
                case 6 -> new UserAccountMenu(user, userDao, scanner).run();
                case 7 -> new LogMenu(scanner).run();
                case 8 -> {
                    updateUserRoleAndStatus();
                    InputHelper.pause(scanner);
                }
                case 0 -> loggedIn = !DashboardView.confirmLogout(scanner);
                default -> {
                    System.out.println("[!] Invalid menu choice.");
                    InputHelper.pause(scanner);
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Update a user's role and status (Administrator only)
    // ------------------------------------------------------------------
    private void updateUserRoleAndStatus() {
        System.out.println();
        System.out.println("UPDATE USER ROLE & STATUS");
        InputHelper.printLine(30, '-');

        List<User> users = userDao.findAll();
        showUserTable(users);
        System.out.println("(Enter 0 at any step to cancel.)");
        System.out.println();

        // Step 1: which user?
        User target = null;
        while (target == null) {
            int targetUserId = readIntInput("Enter the User ID to update (0 to cancel): ");
            if (targetUserId == 0) {
                System.out.println("[!] Update cancelled.");
                return;
            }
            // An admin may not edit the account they are logged in with.
            if (targetUserId == user.id()) {
                System.out.println("WARNING: Modifying the currently active admin account is restricted.");
                continue;
            }
            for (User u : users) {
                if (u.id() == targetUserId) {
                    target = u;
                }
            }
            if (target == null) {
                System.out.println("[!] No user with ID " + targetUserId + ". Please try again.");
            }
        }

        System.out.println();
        System.out.println("Selected user : " + target.username());
        System.out.println("Current role  : " + target.roles());
        System.out.println("Current status: " + target.status());
        System.out.println();

        // Step 2: new role
        String newRole = readValidRole();
        if (newRole == null) {
            System.out.println("[!] Update cancelled.");
            return;
        }

        // Step 3: new status
        String newStatus = readValidStatus();
        if (newStatus == null) {
            System.out.println("[!] Update cancelled.");
            return;
        }

        // Step 4: save
        if (userDao.updateUserRoleAndStatus(target.id(), newRole, newStatus)) {
            logDao.log(user.id(), "UPDATE", "Updated user " + target.username() + ": role "
                    + target.roles() + " -> " + newRole + ", status " + target.status() + " -> " + newStatus);
            System.out.println();
            System.out.println("[OK] " + target.username() + " is now " + newRole + " / " + newStatus + ".");
        } else {
            System.out.println("[!] Could not update the user.");
        }
    }

    private void showUserTable(List<User> users) {
        System.out.println();
        System.out.printf("%-6s %-20s %-18s %-10s%n", "ID", "Username", "Role", "Status");
        InputHelper.printLine(58, '-');
        for (User u : users) {
            String you = (u.id() == user.id()) ? "  (you)" : "";
            System.out.printf("%-6d %-20s %-18s %-10s%s%n", u.id(),
                    InputHelper.cut(u.username(), 20), u.roles(), u.status(), you);
        }
        InputHelper.printLine(58, '-');
    }

    // ------------------------------------------------------------------
    // Input validation wrappers
    // ------------------------------------------------------------------

    /** Keeps asking until a whole number is typed, so letters or symbols cannot crash the program. */
    private int readIntInput(String prompt) {
        while (true) {
            System.out.print(prompt);
            System.out.flush();
            String input = scanner.nextLine().trim();
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("[!] Invalid input. Please enter numbers only.");
            }
        }
    }

    /** Keeps asking until a valid role is typed. Returns null if the admin types 0 (cancel). */
    private String readValidRole() {
        StringBuilder valid = new StringBuilder();
        for (Role r : Role.values()) {
            if (valid.length() > 0) {
                valid.append(", ");
            }
            valid.append(r.name());
        }

        while (true) {
            System.out.println("Valid roles: " + valid);
            System.out.print("Enter new role (0 to cancel): ");
            System.out.flush();
            String input = scanner.nextLine().trim();
            if (input.equals("0")) {
                return null;
            }
            // "property manager" and "PROPERTY_MANAGER" are both accepted.
            String role = input.toUpperCase().replace(' ', '_');
            for (Role r : Role.values()) {
                if (r.name().equals(role)) {
                    return r.name();
                }
            }
            System.out.println("[!] Invalid role. Please type one of the valid roles.");
            System.out.println();
        }
    }

    /**
     * Keeps asking until a valid status is typed. Returns null if the admin types 0 (cancel).
     * "APPROVED" is accepted and saved as ACTIVE, which is the status this system uses.
     */
    private String readValidStatus() {
        while (true) {
            System.out.println("Valid statuses: ACTIVE (same as APPROVED), PENDING, INACTIVE");
            System.out.print("Enter new status (0 to cancel): ");
            System.out.flush();
            String input = scanner.nextLine().trim();
            if (input.equals("0")) {
                return null;
            }
            String status = input.toUpperCase();
            if (status.equals("APPROVED")) {
                status = "ACTIVE";
            }
            if (status.equals("ACTIVE") || status.equals("PENDING") || status.equals("INACTIVE")) {
                return status;
            }
            System.out.println("[!] Invalid status. Please type ACTIVE, PENDING, or INACTIVE.");
            System.out.println();
        }
    }
}
