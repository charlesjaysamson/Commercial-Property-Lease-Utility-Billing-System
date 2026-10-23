package com.nexus.ui;

import com.nexus.dao.BillingDao;
import com.nexus.dao.PropertyDao;
import com.nexus.model.User;

import java.util.Scanner;

/**
 * Small pieces shared by the three role menus, so the same code is not copied three times.
 */
public class DashboardView {

    private DashboardView() {
    }

    public static void printWelcome(User user) {
        System.out.println();
        System.out.println("Welcome, " + user.username() + " (" + user.roles() + ")");
        System.out.println();
    }

    public static void printPropertyOverview(PropertyDao propertyDao) {
        int available = propertyDao.countByStatus("AVAILABLE");
        int occupied = propertyDao.countByStatus("OCCUPIED");
        InputHelper.printLine(60, '-');
        System.out.println("PROPERTY OVERVIEW");
        InputHelper.printLine(60, '-');
        System.out.println("Total Units       : " + (available + occupied));
        System.out.println("Occupied Units    : " + occupied);
        System.out.println("Available Units   : " + available);
        System.out.println();
    }

    public static void printBillingOverview(BillingDao billingDao) {
        int generated = billingDao.count();
        int paid = billingDao.countByStatus("PAID");
        InputHelper.printLine(60, '-');
        System.out.println("BILLING OVERVIEW");
        InputHelper.printLine(60, '-');
        System.out.println("Bills Generated   : " + generated);
        System.out.println("Paid Bills        : " + paid);
        System.out.println("Unpaid Bills      : " + (generated - paid));
        System.out.println();
    }

    /** Asks for confirmation. Returns true if the user really wants to log out. */
    public static boolean confirmLogout(Scanner scanner) {
        if (InputHelper.askYesNo(scanner, "Are you sure you want to logout? (Y/N): ")) {
            System.out.println("You have been logged out.");
            return true;
        }
        return false;
    }
}
