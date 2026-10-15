package ui;

import dao.BillingDao;
import dao.PropertyDao;
import model.Role;
import model.User;

/**
 * Menu shown after login. What each person sees depends on their role
 * (see Section 2 of the proposal).
 */
public class DashboardMenu {

    private final User user;
    private final PropertyDao propertyDao = new PropertyDao();
    private final BillingDao billingDao = new BillingDao();

    public DashboardMenu(User user) {
        this.user = user;
    }

    public void run() {
        // New accounts can log in, but cannot use the system until approved.
        if (!user.isActive()) {
            System.out.println();
            System.out.println("Your account status is " + user.status() + ".");
            System.out.println("An Administrator must approve your account before you can use the system.");
            System.out.println("You will now be logged out.");
            InputHelper.pause();
            return;
        }

        boolean isAdmin = user.hasRole(Role.ADMINISTRATOR);
        boolean propertyAccess = isAdmin || user.hasRole(Role.PROPERTY_MANAGER);
        boolean billingAccess = isAdmin || user.hasRole(Role.BILLING_STAFF);

        boolean loggedIn = true;
        while (loggedIn) {
            // Any bill past its due date becomes OVERDUE and gets its late fee.
            billingDao.markOverdueBills();

            InputHelper.clearScreen();
            InputHelper.printHeader("DASHBOARD");
            System.out.println();
            System.out.println("Welcome, " + user.username() + " (" + user.roles() + ")");
            System.out.println();

            int available = propertyDao.countByStatus("AVAILABLE");
            int occupied = propertyDao.countByStatus("OCCUPIED");
            InputHelper.printLine(60, '-');
            System.out.println("PROPERTY OVERVIEW");
            InputHelper.printLine(60, '-');
            System.out.println("Total Units       : " + (available + occupied));
            System.out.println("Occupied Units    : " + occupied);
            System.out.println("Available Units   : " + available);
            System.out.println();

            int generated = billingDao.count();
            int paid = billingDao.countByStatus("PAID");
            InputHelper.printLine(60, '-');
            System.out.println("BILLING OVERVIEW");
            InputHelper.printLine(60, '-');
            System.out.println("Bills Generated   : " + generated);
            System.out.println("Paid Bills        : " + paid);
            System.out.println("Unpaid Bills      : " + (generated - paid));
            System.out.println();

            InputHelper.printLine(60, '-');
            if (propertyAccess) {
                System.out.println("[1] Property Management");
                System.out.println("[2] Tenant Management");
                System.out.println("[3] Lease Management");
            }
            if (billingAccess) {
                System.out.println("[4] Billing & Payments");
                System.out.println("[5] Reports");
            }
            if (isAdmin) {
                System.out.println("[6] User Accounts");
                System.out.println("[7] Activity Logs");
            }
            System.out.println("[0] Logout");
            InputHelper.printLine(60, '-');

            switch (InputHelper.readInt("Enter choice: ")) {
                case 1 -> open(propertyAccess, () -> new PropertyMenu(user).run());
                case 2 -> open(propertyAccess, () -> new TenantMenu(user).run());
                case 3 -> open(propertyAccess, () -> new LeaseMenu(user).run());
                case 4 -> open(billingAccess, () -> new BillingMenu(user).run());
                case 5 -> open(billingAccess, () -> new ReportMenu().run());
                case 6 -> open(isAdmin, () -> new UserAccountMenu(user).run());
                case 7 -> open(isAdmin, () -> new LogMenu().run());
                case 0 -> {
                    if (InputHelper.askYesNo("Are you sure you want to logout? (Y/N): ")) {
                        System.out.println("You have been logged out.");
                        loggedIn = false;
                    }
                }
                default -> {
                    System.out.println("[!] Invalid menu choice.");
                    InputHelper.pause();
                }
            }
        }
    }

    // Opens a screen only if this role is allowed to use it.
    private void open(boolean allowed, Runnable screen) {
        if (allowed) {
            screen.run();
        } else {
            System.out.println("[!] Access denied. Your role (" + user.roles() + ") cannot open this screen.");
            InputHelper.pause();
        }
    }
}
