package com.nexus.ui;

import com.nexus.dao.BillingDao;
import com.nexus.model.User;

import java.util.Scanner;

/**
 * Menu for the BILLING_STAFF role: manages billing and utility records.
 */
public class BillingStaffMenu {

    private final User user;
    private final Scanner scanner;      // the ONE shared Scanner from Main
    private final BillingDao billingDao = new BillingDao();

    public BillingStaffMenu(User user, Scanner scanner) {
        this.user = user;
        this.scanner = scanner;
    }

    public void run() {
        boolean loggedIn = true;
        while (loggedIn) {
            // Any bill past its due date becomes OVERDUE and gets its late fee.
            billingDao.markOverdueBills();

            InputHelper.clearScreen();
            InputHelper.printHeader("BILLING STAFF DASHBOARD");
            DashboardView.printWelcome(user);
            DashboardView.printBillingOverview(billingDao);

            InputHelper.printLine(60, '-');
            System.out.println("[1] Billing & Payments");
            System.out.println("[2] Reports");
            System.out.println("[0] Logout");
            InputHelper.printLine(60, '-');

            switch (InputHelper.readInt(scanner, "Enter choice: ")) {
                case 1 -> new BillingMenu(user, scanner).run();
                case 2 -> new ReportMenu(scanner).run();
                case 0 -> loggedIn = !DashboardView.confirmLogout(scanner);
                default -> {
                    System.out.println("[!] Invalid menu choice.");
                    InputHelper.pause(scanner);
                }
            }
        }
    }
}
