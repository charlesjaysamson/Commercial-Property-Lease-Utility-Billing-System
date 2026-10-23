package com.nexus.ui;

import com.nexus.dao.ActivityLogDao;
import com.nexus.dao.LeaseDao;
import com.nexus.dao.TenantDao;
import com.nexus.model.Tenant;
import com.nexus.model.User;

import java.util.List;
import java.util.Scanner;

/**
 * Tenant management. "Delete" is a soft-delete: status becomes INACTIVE.
 */
public class TenantMenu {

    private final User user;
    private final Scanner scanner;
    private final TenantDao tenantDao = new TenantDao();
    private final LeaseDao leaseDao = new LeaseDao();
    private final ActivityLogDao logDao = new ActivityLogDao();

    public TenantMenu(User user, Scanner scanner) {
        this.user = user;
        this.scanner = scanner;
    }

    public void run() {
        boolean back = false;
        while (!back) {
            InputHelper.clearScreen();
            InputHelper.printHeader("TENANT MANAGEMENT");
            System.out.println();
            System.out.println("[1] View Tenants");
            System.out.println("[2] Add Tenant");
            System.out.println("[3] Search Tenants");
            System.out.println("[4] Update Tenant Information");
            System.out.println("[5] Deactivate Tenant (Soft-Delete)");
            System.out.println("[6] Reactivate Tenant");
            System.out.println("[0] Back");
            InputHelper.printLine(60, '-');

            switch (InputHelper.readInt(scanner, "Enter choice: ")) {
                case 1 -> {
                    showTable(tenantDao.findAll());
                    InputHelper.pause(scanner);
                }
                case 2 -> {
                    addTenant();
                    InputHelper.pause(scanner);
                }
                case 3 -> {
                    searchTenants();
                    InputHelper.pause(scanner);
                }
                case 4 -> {
                    updateTenant();
                    InputHelper.pause(scanner);
                }
                case 5 -> {
                    deactivateTenant();
                    InputHelper.pause(scanner);
                }
                case 6 -> {
                    reactivateTenant();
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

    private void showTable(List<Tenant> list) {
        System.out.println();
        if (list.isEmpty()) {
            System.out.println("No tenants found.");
            return;
        }
        System.out.printf("%-4s %-18s %-18s %-13s %-22s %-8s%n",
                "No.", "Tenant", "Business", "Contact", "Email", "Status");
        InputHelper.printLine(88, '-');
        for (int i = 0; i < list.size(); i++) {
            Tenant t = list.get(i);
            System.out.printf("%-4d %-18s %-18s %-13s %-22s %-8s%n", i + 1,
                    InputHelper.cut(t.tenantName(), 18), InputHelper.cut(t.businessName(), 18),
                    InputHelper.cut(t.contactNumber(), 13), InputHelper.cut(t.email(), 22), t.status());
        }
        InputHelper.printLine(88, '-');
    }

    private void addTenant() {
        System.out.println();
        System.out.println("ADD NEW TENANT");
        InputHelper.printLine(20, '-');

        String name = InputHelper.readText(scanner, "Tenant Name    : ");
        String business = InputHelper.readText(scanner, "Business Name  : ");
        String contact = InputHelper.readText(scanner, "Contact Number : ");
        String email = InputHelper.readEmail(scanner, "Email          : ");

        if (tenantDao.add(name, business, contact, email)) {
            logDao.log(user.id(), "CREATE", "Added tenant " + name + " (" + business + ")");
            System.out.println();
            System.out.println("[OK] Tenant " + name + " has been added (status: ACTIVE).");
        } else {
            System.out.println("[!] Could not add the tenant.");
        }
    }

    private void searchTenants() {
        System.out.println();
        String keyword = InputHelper.readText(scanner, "Search (tenant or business name): ");
        showTable(tenantDao.search(keyword));
    }

    private void updateTenant() {
        System.out.println();
        List<Tenant> list = tenantDao.findAll();
        if (list.isEmpty()) {
            System.out.println("No tenants to update.");
            return;
        }
        showTable(list);
        int n = InputHelper.chooseNumber(scanner, "Enter tenant number to update (0 to cancel): ", list.size());
        if (n == 0) {
            System.out.println("[!] Cancelled.");
            return;
        }
        Tenant t = list.get(n - 1);

        System.out.println();
        System.out.println("Press ENTER to keep the current value.");
        String name = InputHelper.readTextOrKeep(scanner, "Tenant Name    [" + t.tenantName() + "]: ", t.tenantName());
        String business = InputHelper.readTextOrKeep(scanner, "Business Name  [" + t.businessName() + "]: ", t.businessName());
        String contact = InputHelper.readTextOrKeep(scanner, "Contact Number [" + t.contactNumber() + "]: ", t.contactNumber());
        String email = InputHelper.readEmailOrKeep(scanner, "Email          [" + t.email() + "]: ", t.email());

        if (tenantDao.updateInfo(t.tenantId(), name, business, contact, email)) {
            logDao.log(user.id(), "UPDATE", "Updated tenant information for " + name);
            System.out.println();
            System.out.println("[OK] Tenant information updated.");
        } else {
            System.out.println("[!] Could not update the tenant.");
        }
    }

    private void deactivateTenant() {
        System.out.println();
        List<Tenant> list = tenantDao.findByStatus("ACTIVE");
        if (list.isEmpty()) {
            System.out.println("No ACTIVE tenants to deactivate.");
            return;
        }
        showTable(list);
        int n = InputHelper.chooseNumber(scanner, "Enter tenant number to deactivate (0 to cancel): ", list.size());
        if (n == 0) {
            System.out.println("[!] Cancelled.");
            return;
        }
        Tenant t = list.get(n - 1);

        // A tenant with a running lease cannot be removed, or the lease would point at nobody.
        if (leaseDao.tenantHasActiveLease(t.tenantId())) {
            System.out.println("[!] " + t.tenantName() + " still has an ACTIVE lease. End the lease first.");
            return;
        }
        if (!InputHelper.askYesNo(scanner, "Deactivate tenant " + t.tenantName() + "? (Y/N): ")) {
            System.out.println("[!] Cancelled.");
            return;
        }
        if (tenantDao.updateStatus(t.tenantId(), "INACTIVE")) {
            logDao.log(user.id(), "DELETE", "Tenant " + t.tenantName() + " status changed to INACTIVE");
            System.out.println("[OK] " + t.tenantName() + " is now INACTIVE.");
        } else {
            System.out.println("[!] Could not update the tenant.");
        }
    }

    private void reactivateTenant() {
        System.out.println();
        List<Tenant> list = tenantDao.findByStatus("INACTIVE");
        if (list.isEmpty()) {
            System.out.println("No INACTIVE tenants to reactivate.");
            return;
        }
        showTable(list);
        int n = InputHelper.chooseNumber(scanner, "Enter tenant number to reactivate (0 to cancel): ", list.size());
        if (n == 0) {
            System.out.println("[!] Cancelled.");
            return;
        }
        Tenant t = list.get(n - 1);
        if (tenantDao.updateStatus(t.tenantId(), "ACTIVE")) {
            logDao.log(user.id(), "UPDATE", "Tenant " + t.tenantName() + " status changed to ACTIVE");
            System.out.println("[OK] " + t.tenantName() + " is now ACTIVE.");
        } else {
            System.out.println("[!] Could not update the tenant.");
        }
    }
}
