package com.nexus.ui;

import com.nexus.dao.ActivityLogDao;
import com.nexus.dao.LeaseDao;
import com.nexus.dao.PropertyDao;
import com.nexus.dao.TenantDao;
import com.nexus.model.Lease;
import com.nexus.model.Property;
import com.nexus.model.Tenant;
import com.nexus.model.User;

import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;

/**
 * Lease management. Ending a lease is a soft-delete: status becomes EXPIRED or TERMINATED.
 */
public class LeaseMenu {

    private final User user;
    private final Scanner scanner;
    private final LeaseDao leaseDao = new LeaseDao();
    private final TenantDao tenantDao = new TenantDao();
    private final PropertyDao propertyDao = new PropertyDao();
    private final ActivityLogDao logDao = new ActivityLogDao();

    public LeaseMenu(User user, Scanner scanner) {
        this.user = user;
        this.scanner = scanner;
    }

    public void run() {
        boolean back = false;
        while (!back) {
            InputHelper.clearScreen();
            InputHelper.printHeader("LEASE MANAGEMENT");
            System.out.println();
            System.out.println("[1] Create New Lease");
            System.out.println("[2] View Leases");
            System.out.println("[3] Update Lease (End Date / Monthly Rent)");
            System.out.println("[4] End Lease (Soft-Delete: Expired / Terminated)");
            System.out.println("[0] Back");
            InputHelper.printLine(60, '-');

            switch (InputHelper.readInt(scanner, "Enter choice: ")) {
                case 1 -> {
                    createLease();
                    InputHelper.pause(scanner);
                }
                case 2 -> {
                    showTable(leaseDao.findAll());
                    InputHelper.pause(scanner);
                }
                case 3 -> {
                    updateLease();
                    InputHelper.pause(scanner);
                }
                case 4 -> {
                    endLease();
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

    private void showTable(List<Lease> list) {
        System.out.println();
        if (list.isEmpty()) {
            System.out.println("No leases found.");
            return;
        }
        System.out.printf("%-4s %-18s %-12s %-11s %-11s %-14s %-10s%n",
                "No.", "Business", "Property", "Start", "End", "Monthly Rent", "Status");
        InputHelper.printLine(86, '-');
        for (int i = 0; i < list.size(); i++) {
            Lease l = list.get(i);
            System.out.printf("%-4d %-18s %-12s %-11s %-11s %-14s %-10s%n", i + 1,
                    InputHelper.cut(l.businessName(), 18), InputHelper.cut(l.propertyName(), 12),
                    InputHelper.formatDate(l.startDate()), InputHelper.formatDate(l.endDate()),
                    InputHelper.peso(l.monthlyRent()), l.status());
        }
        InputHelper.printLine(86, '-');
    }

    private void createLease() {
        System.out.println();
        List<Tenant> tenants = tenantDao.findByStatus("ACTIVE");
        if (tenants.isEmpty()) {
            System.out.println("[!] No ACTIVE tenants. Please add a tenant first.");
            return;
        }
        List<Property> properties = propertyDao.findByStatus("AVAILABLE");
        if (properties.isEmpty()) {
            System.out.println("[!] No AVAILABLE properties. Please add a property or end a lease first.");
            return;
        }

        System.out.println("CREATE NEW LEASE");
        InputHelper.printLine(20, '-');

        System.out.println("Select a tenant:");
        for (int i = 0; i < tenants.size(); i++) {
            Tenant t = tenants.get(i);
            System.out.println("[" + (i + 1) + "] " + t.tenantName() + " (" + t.businessName() + ")");
        }
        int tn = InputHelper.chooseNumber(scanner, "Enter tenant number (0 to cancel): ", tenants.size());
        if (tn == 0) {
            System.out.println("[!] Cancelled.");
            return;
        }
        Tenant tenant = tenants.get(tn - 1);

        System.out.println();
        System.out.println("Select an available property:");
        for (int i = 0; i < properties.size(); i++) {
            Property p = properties.get(i);
            System.out.println("[" + (i + 1) + "] " + p.propertyName() + " - " + p.location()
                    + " - " + InputHelper.peso(p.rentalRate()));
        }
        int pn = InputHelper.chooseNumber(scanner, "Enter property number (0 to cancel): ", properties.size());
        if (pn == 0) {
            System.out.println("[!] Cancelled.");
            return;
        }
        Property property = properties.get(pn - 1);

        System.out.println();
        LocalDate start = InputHelper.readDate(scanner, "Start Date (MM/DD/YYYY) : ");
        LocalDate end;
        while (true) {
            end = InputHelper.readDate(scanner, "End Date   (MM/DD/YYYY) : ");
            if (end.isAfter(start)) {
                break;
            }
            System.out.println("[!] End date must be after the start date.");
        }
        Double customRent = InputHelper.readOptionalPositiveDouble(scanner, 
                "Monthly Rent (ENTER to use " + InputHelper.peso(property.rentalRate()) + ") : ");
        double rent = (customRent == null) ? property.rentalRate() : customRent;

        System.out.println();
        System.out.println("Tenant       : " + tenant.tenantName());
        System.out.println("Business     : " + tenant.businessName());
        System.out.println("Property     : " + property.propertyName());
        System.out.println("Start Date   : " + InputHelper.formatDate(start));
        System.out.println("End Date     : " + InputHelper.formatDate(end));
        System.out.println("Monthly Rent : " + InputHelper.peso(rent));
        if (!InputHelper.askYesNo(scanner, "Create this lease? (Y/N): ")) {
            System.out.println("[!] Cancelled.");
            return;
        }

        if (leaseDao.createLease(property.propertyId(), tenant.tenantId(), start, end, rent)) {
            logDao.log(user.id(), "CREATE", "Created lease: " + tenant.businessName() + " -> "
                    + property.propertyName() + " (" + InputHelper.peso(rent) + "/month)");
            System.out.println();
            System.out.println("[OK] Lease created. " + property.propertyName() + " is now OCCUPIED.");
        } else {
            System.out.println("[!] Could not create the lease. The property may no longer be available.");
        }
    }

    private void updateLease() {
        System.out.println();
        List<Lease> leases = leaseDao.findActive();
        if (leases.isEmpty()) {
            System.out.println("No ACTIVE leases to update.");
            return;
        }
        showTable(leases);
        int n = InputHelper.chooseNumber(scanner, "Enter lease number to update (0 to cancel): ", leases.size());
        if (n == 0) {
            System.out.println("[!] Cancelled.");
            return;
        }
        Lease lease = leases.get(n - 1);

        System.out.println();
        System.out.println("Press ENTER to keep the current value.");
        LocalDate newEnd;
        while (true) {
            newEnd = InputHelper.readOptionalDate(scanner, "New End Date (MM/DD/YYYY) [" + InputHelper.formatDate(lease.endDate()) + "]: ");
            if (newEnd == null || newEnd.isAfter(lease.startDate())) {
                break;
            }
            System.out.println("[!] End date must be after the start date (" + InputHelper.formatDate(lease.startDate()) + ").");
        }
        Double newRent = InputHelper.readOptionalPositiveDouble(scanner, "New Monthly Rent [" + InputHelper.peso(lease.monthlyRent()) + "]: ");

        if (newEnd == null && newRent == null) {
            System.out.println("[!] No changes made.");
            return;
        }
        LocalDate finalEnd = (newEnd == null) ? lease.endDate() : newEnd;
        double finalRent = (newRent == null) ? lease.monthlyRent() : newRent;

        if (leaseDao.updateTerms(lease.leaseId(), finalEnd, finalRent)) {
            logDao.log(user.id(), "UPDATE", "Updated lease of " + lease.businessName() + " ("
                    + lease.propertyName() + "): end " + InputHelper.formatDate(finalEnd)
                    + ", rent " + InputHelper.peso(finalRent));
            System.out.println();
            System.out.println("[OK] Lease updated. Existing bills are not changed; new bills use the new rent.");
        } else {
            System.out.println("[!] Could not update the lease.");
        }
    }

    private void endLease() {
        System.out.println();
        List<Lease> leases = leaseDao.findActive();
        if (leases.isEmpty()) {
            System.out.println("No ACTIVE leases to end.");
            return;
        }
        showTable(leases);
        int n = InputHelper.chooseNumber(scanner, "Enter lease number to end (0 to cancel): ", leases.size());
        if (n == 0) {
            System.out.println("[!] Cancelled.");
            return;
        }
        Lease lease = leases.get(n - 1);

        System.out.println();
        System.out.println("[1] EXPIRED    (lease ran its full term)");
        System.out.println("[2] TERMINATED (ended early)");
        int s = InputHelper.chooseNumber(scanner, "Choose how it ended (0 to cancel): ", 2);
        if (s == 0) {
            System.out.println("[!] Cancelled.");
            return;
        }
        String newStatus = (s == 1) ? "EXPIRED" : "TERMINATED";

        if (!InputHelper.askYesNo(scanner, "Mark the lease of " + lease.businessName() + " as " + newStatus + "? (Y/N): ")) {
            System.out.println("[!] Cancelled.");
            return;
        }
        if (leaseDao.endLease(lease.leaseId(), lease.propertyId(), newStatus)) {
            logDao.log(user.id(), "DELETE", "Lease of " + lease.businessName() + " (" + lease.propertyName()
                    + ") status changed to " + newStatus);
            System.out.println();
            System.out.println("[OK] Lease is now " + newStatus + ". " + lease.propertyName() + " is AVAILABLE again.");
        } else {
            System.out.println("[!] Could not end the lease.");
        }
    }
}
