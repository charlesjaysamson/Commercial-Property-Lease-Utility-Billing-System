package ui;

import dao.ActivityLogDao;
import dao.UserDao;
import model.User;

import java.util.List;

/**
 * Administrator-only screen: approve, deactivate, and reactivate accounts.
 */
public class UserAccountMenu {

    private final User admin;
    private final UserDao userDao = new UserDao();
    private final ActivityLogDao logDao = new ActivityLogDao();

    public UserAccountMenu(User admin) {
        this.admin = admin;
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

            switch (InputHelper.readInt("Enter choice: ")) {
                case 1 -> {
                    showTable(userDao.findAll());
                    InputHelper.pause();
                }
                case 2 -> {
                    changeStatus("PENDING", "ACTIVE", "approve");
                    InputHelper.pause();
                }
                case 3 -> {
                    changeStatus("ACTIVE", "INACTIVE", "deactivate");
                    InputHelper.pause();
                }
                case 4 -> {
                    changeStatus("INACTIVE", "ACTIVE", "reactivate");
                    InputHelper.pause();
                }
                case 0 -> back = true;
                default -> {
                    System.out.println("[!] Invalid menu choice.");
                    InputHelper.pause();
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
        int n = InputHelper.chooseNumber("Enter account number to " + verb + " (0 to cancel): ", list.size());
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
