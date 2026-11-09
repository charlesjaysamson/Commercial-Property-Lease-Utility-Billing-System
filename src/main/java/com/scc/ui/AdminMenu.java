package com.scc.ui;

import com.scc.dao.ActivityLogDao;
import com.scc.dao.BillingDao;
import com.scc.dao.PropertyDao;
import com.scc.dao.UserDao;
import com.scc.model.ActivityLog;
import com.scc.model.Role;
import com.scc.model.User;

import java.util.ArrayList;
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
            System.out.println("[7] View System Audit Logs");
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
                case 7 -> viewAuditLogs();
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
        // The DAO changes the user AND writes the audit log entry.
        if (userDao.updateUserRoleAndStatus(user, target, newRole, newStatus)) {
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
    // System Audit Logs (Administrator only)
    // ------------------------------------------------------------------
    private void viewAuditLogs() {
        boolean back = false;
        while (!back) {
            InputHelper.clearScreen();
            InputHelper.printHeader("SYSTEM AUDIT LOGS");
            System.out.println();
            System.out.println("[1] View All System Logs");
            System.out.println("[2] Filter Logs by User ID");
            System.out.println("[0] Back");
            InputHelper.printLine(60, '-');

            switch (readIntInput("Enter choice: ")) {
                case 1 -> {
                    System.out.println();
                    printLogTable(logDao.getAllLogs());
                    InputHelper.pause(scanner);
                }
                case 2 -> {
                    filterLogsByUser();
                    InputHelper.pause(scanner);
                }
                case 0 -> back = true;
                default -> {
                    System.out.println("[!] Invalid menu choice.");
                    InputHelper.pause(scanner);
                }
            }
        }
    }

    private void filterLogsByUser() {
        System.out.println();
        List<User> users = userDao.findAll();
        showUserTable(users);

        int userId = readIntInput("Enter the User ID to filter by (0 to cancel): ");
        if (userId == 0) {
            System.out.println("[!] Cancelled.");
            return;
        }
        User found = null;
        for (User u : users) {
            if (u.id() == userId) {
                found = u;
            }
        }
        if (found == null) {
            System.out.println("[!] No user with ID " + userId + ".");
            return;
        }

        System.out.println();
        System.out.println("Logs of " + found.username() + " (User ID " + userId + ")");
        printLogTable(logDao.getLogsByUserId(userId));
    }

    private void printLogTable(List<ActivityLog> logs) {
        if (logs.isEmpty()) {
            System.out.println("No logs found.");
            return;
        }
        String format = "%-5s | %-8s | %-18s | %-35s | %-20s%n";
        System.out.printf(format, "ID", "User ID", "Action Type", "Description", "Timestamp");
        InputHelper.printLine(98, '-');
        for (ActivityLog log : logs) {
            // A long description continues on the next line(s) inside its own column.
            List<String> lines = wrapText(log.description(), 35);
            System.out.printf(format, log.id(), log.userId(),
                    InputHelper.cut(log.actionType(), 18), lines.get(0), log.timestamp());
            for (int i = 1; i < lines.size(); i++) {
                System.out.printf(format, "", "", "", lines.get(i), "");
            }
        }
        InputHelper.printLine(98, '-');
        System.out.println(logs.size() + " log(s) shown, newest first.");
    }

    /** Splits text into lines of at most "width" characters, breaking at spaces. */
    private List<String> wrapText(String text, int width) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            while (word.length() > width) {
                if (line.length() > 0) {
                    lines.add(line.toString());
                    line = new StringBuilder();
                }
                lines.add(word.substring(0, width));
                word = word.substring(width);
            }
            if (line.length() == 0) {
                line.append(word);
            } else if (line.length() + 1 + word.length() <= width) {
                line.append(' ').append(word);
            } else {
                lines.add(line.toString());
                line = new StringBuilder(word);
            }
        }
        if (line.length() > 0) {
            lines.add(line.toString());
        }
        if (lines.isEmpty()) {
            lines.add("");
        }
        return lines;
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
