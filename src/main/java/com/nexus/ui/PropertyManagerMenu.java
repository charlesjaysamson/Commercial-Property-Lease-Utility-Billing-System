package com.nexus.ui;

import com.nexus.dao.PropertyDao;
import com.nexus.model.User;

import java.util.Scanner;

/**
 * Menu for the PROPERTY_MANAGER role: manages properties, tenants, and leases.
 */
public class PropertyManagerMenu {

    private final User user;
    private final Scanner scanner;      // the ONE shared Scanner from Main
    private final PropertyDao propertyDao = new PropertyDao();

    public PropertyManagerMenu(User user, Scanner scanner) {
        this.user = user;
        this.scanner = scanner;
    }

    public void run() {
        boolean loggedIn = true;
        while (loggedIn) {
            InputHelper.clearScreen();
            InputHelper.printHeader("PROPERTY MANAGER DASHBOARD");
            DashboardView.printWelcome(user);
            DashboardView.printPropertyOverview(propertyDao);

            InputHelper.printLine(60, '-');
            System.out.println("[1] Property Management");
            System.out.println("[2] Tenant Management");
            System.out.println("[3] Lease Management");
            System.out.println("[0] Logout");
            InputHelper.printLine(60, '-');

            switch (InputHelper.readInt(scanner, "Enter choice: ")) {
                case 1 -> new PropertyMenu(user, scanner).run();
                case 2 -> new TenantMenu(user, scanner).run();
                case 3 -> new LeaseMenu(user, scanner).run();
                case 0 -> loggedIn = !DashboardView.confirmLogout(scanner);
                default -> {
                    System.out.println("[!] Invalid menu choice.");
                    InputHelper.pause(scanner);
                }
            }
        }
    }
}
