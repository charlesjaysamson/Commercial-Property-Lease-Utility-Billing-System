package com.nexus.ui;

import com.nexus.dao.BillingDao;
import com.nexus.dao.PropertyDao;
import com.nexus.dao.UserDao;
import com.nexus.model.User;

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
                case 0 -> loggedIn = !DashboardView.confirmLogout(scanner);
                default -> {
                    System.out.println("[!] Invalid menu choice.");
                    InputHelper.pause(scanner);
                }
            }
        }
    }
}
