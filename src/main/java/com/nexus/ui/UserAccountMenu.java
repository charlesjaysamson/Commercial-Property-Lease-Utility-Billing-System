package com.nexus.ui;

import com.nexus.dao.ActivityLogDao;
import com.nexus.dao.UserDao;
import com.nexus.model.User;

import java.util.List;
import java.util.Scanner;

/**
 * Administrator-only screen: approve, deactivate, and reactivate accounts.
 */
public class UserAccountMenu {

    private final User admin;
    private final UserDao userDao;      // the ONE shared UserDao from Main
    private final Scanner scanner;      // the ONE shared Scanner from Main
    private final ActivityLogDao logDao = new ActivityLogDao();

    public UserAccountMenu(User admin, UserDao userDao, Scanner scanner) {
        this.admin = admin;
        this.userDao = userDao;
        this.scanner = scanner;
    }

    public void run() {
        boolean back = false;
        while (!back) {
            InputHelper.clearScreen();
            InputHelper.printHeader("USER ACCOUNTS");
            System.out.println();
            System.out.println("[1] View All Accounts");
            System.out.println("[2] Approve Pending Account");
            System.out.println("[3] Deactivate Account");
            System.out.println("[4] Reactivate Account");
            System.out.println("[0] Back");
            InputHelper.printLine(60, '-');

            switch (InputHelper.readInt(scanner, "Enter choice: ")) {
                case 1 -> {
                    showTable(userDao.findAll());
                    InputHelper.pause(scanner);
                }
                case 2 -> {
                    changeStatus("PENDING", "ACTIVE", "approve");
                    InputHelper.pause(scanner);
                }
                case 3 -> {
                    changeStatus("ACTIVE", "INACTIVE", "deactivate");
                    InputHelper.pause(scanner);
                }
                case 4 -> {
                    changeStatus("INACTIVE", "ACTIVE", "reactivate");
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

    private void showTable(List<User> list) {
        System.out.println();
        if (list.isEmpty()) {
            System.out.println("No accounts found.");
            return;
        }
        System.out.printf("%-4s %-20s %-18s %-10s%n", "No.", "Username", "Role", "Status");
        InputHelper.printLine(56, '-');
        for (int i = 0; i < list.size(); i++) {
            User u = list.get(i);
            System.out.printf("%-4d %-20s %-18s %-10s%n", i + 1,
                    InputHelper.cut(u.username(), 20), u.roles(), u.status());
        }
        InputHelper.printLine(56, '-');
    }

    private void changeStatus(String fromStatus, String toStatus, String verb) {
        System.out.println();
        List<User> list = userDao.findByStatus(fromStatus);
        // An administrator must not deactivate their own account.
        list.removeIf(u -> u.id() == admin.id());

        if (list.isEmpty()) {
            System.out.println("There are no " + fromStatus + " accounts to " + verb + ".");
            return;
        }
        showTable(list);
        int n = InputHelper.chooseNumber(scanner, "Enter account number to " + verb + " (0 to cancel): ", list.size());
        if (n == 0) {
            System.out.println("[!] Cancelled.");
            return;
        }
        User target = list.get(n - 1);

        if (userDao.updateStatus(target.id(), toStatus)) {
            logDao.log(admin.id(), "UPDATE", "Account " + target.username() + " status changed to " + toStatus);
            System.out.println("[OK] " + target.username() + " is now " + toStatus + ".");
        } else {
            System.out.println("[!] Could not update the account.");
        }
    }
}
