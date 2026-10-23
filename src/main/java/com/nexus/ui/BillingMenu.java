package com.nexus.ui;

import com.nexus.dao.ActivityLogDao;
import com.nexus.dao.BillingDao;
import com.nexus.dao.LeaseDao;
import com.nexus.model.Billing;
import com.nexus.model.Lease;
import com.nexus.model.User;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Scanner;

/**
 * Utility metering, monthly billing, and payment processing.
 * Every bill belongs to a lease, so it always uses that lease's rent.
 */
public class BillingMenu {

    private final User user;
    private final Scanner scanner;
    private final BillingDao billingDao = new BillingDao();
    private final LeaseDao leaseDao = new LeaseDao();
    private final ActivityLogDao logDao = new ActivityLogDao();

    public BillingMenu(User user, Scanner scanner) {
        this.user = user;
        this.scanner = scanner;
    }

    public void run() {
        boolean back = false;
        while (!back) {
            billingDao.markOverdueBills();

            InputHelper.clearScreen();
            InputHelper.printHeader("BILLING & PAYMENTS");
            System.out.println();
            System.out.println("[1] Generate Monthly Bill (with Utility Metering)");
            System.out.println("[2] View Bills");
            System.out.println("[3] Process Payment");
            System.out.println("[0] Back");
            InputHelper.printLine(60, '-');

            switch (InputHelper.readInt(scanner, "Enter choice: ")) {
                case 1 -> {
                    generateBill();
                    InputHelper.pause(scanner);
                }
                case 2 -> {
                    showBills(billingDao.findAll());
                    InputHelper.pause(scanner);
                }
                case 3 -> {
                    processPayment();
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

    private void showBills(List<Billing> list) {
        System.out.println();
        if (list.isEmpty()) {
            System.out.println("No bills found.");
            return;
        }
        System.out.printf("%-4s %-16s %-10s %-15s %-12s %-12s %-10s %-13s %-8s%n",
                "No.", "Business", "Property", "Period", "Rent", "Utilities", "Late Fee", "Total Due", "Status");
        InputHelper.printLine(108, '-');
        for (int i = 0; i < list.size(); i++) {
            Billing b = list.get(i);
            System.out.printf("%-4d %-16s %-10s %-15s %-12s %-12s %-10s %-13s %-8s%n", i + 1,
                    InputHelper.cut(b.businessName(), 16), InputHelper.cut(b.propertyName(), 10),
                    InputHelper.formatPeriod(b.billingPeriod()), InputHelper.peso(b.rentAmount()),
                    InputHelper.peso(b.utilityAmount()), InputHelper.peso(b.lateFee()),
                    InputHelper.peso(b.totalAmount()), b.paymentStatus());
        }
        InputHelper.printLine(108, '-');
    }

    // Reads a meter reading that cannot be lower than the previous one.
    private double readCurrentReading(String prompt, double previous) {
        while (true) {
            double current = InputHelper.readNonNegativeDouble(scanner, prompt);
            if (current >= previous) {
                return current;
            }
            System.out.println("[!] Current reading cannot be lower than the previous reading.");
        }
    }

    private void generateBill() {
        System.out.println();
        List<Lease> leases = leaseDao.findActive();
        if (leases.isEmpty()) {
            System.out.println("[!] No ACTIVE leases. A lease must be created before billing.");
            return;
        }

        System.out.println("GENERATE MONTHLY BILL");
        InputHelper.printLine(25, '-');
        System.out.println("Select a lease to bill:");
        for (int i = 0; i < leases.size(); i++) {
            Lease l = leases.get(i);
            System.out.println("[" + (i + 1) + "] " + l.businessName() + " - " + l.propertyName()
                    + " (" + InputHelper.peso(l.monthlyRent()) + "/month)");
        }
        int n = InputHelper.chooseNumber(scanner, "Enter lease number (0 to cancel): ", leases.size());
        if (n == 0) {
            System.out.println("[!] Cancelled.");
            return;
        }
        Lease lease = leases.get(n - 1);

        YearMonth month = InputHelper.readMonth(scanner, "Billing Month (YYYY-MM, e.g. 2026-09): ");
        String period = month.toString();
        if (billingDao.existsForLeaseAndPeriod(lease.leaseId(), period)) {
            System.out.println("[!] " + lease.businessName() + " already has a bill for "
                    + InputHelper.formatPeriod(period) + ".");
            return;
        }

        // ----- ELECTRICITY -----
        System.out.println();
        System.out.println("ELECTRICITY");
        double prevElec = InputHelper.readNonNegativeDouble(scanner, "Previous Reading (kWh) : ");
        double currElec = readCurrentReading("Current Reading (kWh)  : ", prevElec);
        double rateElec = InputHelper.readNonNegativeDouble(scanner, "Rate per kWh            : ");
        double elecConsumption = currElec - prevElec;                         // Consumption = Current - Previous
        double elecBill = BillingDao.round2(elecConsumption * rateElec);      // Bill = Consumption x Rate

        // ----- WATER -----
        System.out.println();
        System.out.println("WATER");
        double prevWater = InputHelper.readNonNegativeDouble(scanner, "Previous Reading (m3)  : ");
        double currWater = readCurrentReading("Current Reading (m3)   : ", prevWater);
        double rateWater = InputHelper.readNonNegativeDouble(scanner, "Rate per m3             : ");
        double waterConsumption = currWater - prevWater;
        double waterBill = BillingDao.round2(waterConsumption * rateWater);

        System.out.println();
        LocalDate billingDate = LocalDate.now();
        LocalDate dueDate = InputHelper.readDate(scanner, "Due Date (MM/DD/YYYY)  : ");

        double rent = lease.monthlyRent();                                    // rent comes from the lease
        double utility = BillingDao.round2(elecBill + waterBill);
        double total = BillingDao.round2(rent + utility);

        System.out.println();
        InputHelper.printLine(48, '-');
        System.out.println("Tenant   : " + lease.tenantName());
        System.out.println("Business : " + lease.businessName());
        System.out.println("Property : " + lease.propertyName());
        System.out.println("Period   : " + InputHelper.formatPeriod(period));
        System.out.println("Due Date : " + InputHelper.formatDate(dueDate));
        System.out.println();
        System.out.printf("%-32s %15s%n", "Monthly Rent", InputHelper.peso(rent));
        System.out.printf("%-32s %15s%n", "Electricity (" + elecConsumption + " kWh)", InputHelper.peso(elecBill));
        System.out.printf("%-32s %15s%n", "Water (" + waterConsumption + " m3)", InputHelper.peso(waterBill));
        InputHelper.printLine(48, '-');
        System.out.printf("%-32s %15s%n", "TOTAL AMOUNT DUE", InputHelper.peso(total));
        InputHelper.printLine(48, '-');
        System.out.println("A " + (int) (BillingDao.LATE_FEE_RATE * 100)
                + "% late fee is added automatically if this bill is unpaid after the due date.");

        if (!InputHelper.askYesNo(scanner, "Save this bill? (Y/N): ")) {
            System.out.println("[!] Cancelled.");
            return;
        }
        if (billingDao.create(lease.leaseId(), period, rent, utility, billingDate, dueDate)) {
            logDao.log(user.id(), "CREATE", "Generated bill for " + lease.businessName() + " ("
                    + InputHelper.formatPeriod(period) + "), total " + InputHelper.peso(total));
            System.out.println();
            System.out.println("[OK] Bill saved. Use Process Payment to settle it.");
        } else {
            System.out.println("[!] Could not save the bill.");
        }
    }

    private void processPayment() {
        System.out.println();
        List<Billing> bills = billingDao.findUnsettled();
        if (bills.isEmpty()) {
            System.out.println("There are no unpaid bills.");
            return;
        }

        System.out.println("PROCESS PAYMENT");
        InputHelper.printLine(20, '-');
        System.out.printf("%-4s %-16s %-10s %-15s %-13s %-11s %-8s%n",
                "No.", "Business", "Property", "Period", "Total Due", "Due Date", "Status");
        InputHelper.printLine(82, '-');
        for (int i = 0; i < bills.size(); i++) {
            Billing b = bills.get(i);
            System.out.printf("%-4d %-16s %-10s %-15s %-13s %-11s %-8s%n", i + 1,
                    InputHelper.cut(b.businessName(), 16), InputHelper.cut(b.propertyName(), 10),
                    InputHelper.formatPeriod(b.billingPeriod()), InputHelper.peso(b.totalAmount()),
                    InputHelper.formatDate(b.dueDate()), b.paymentStatus());
        }
        InputHelper.printLine(82, '-');

        int n = InputHelper.chooseNumber(scanner, "Enter bill number to pay (0 to cancel): ", bills.size());
        if (n == 0) {
            System.out.println("[!] Cancelled.");
            return;
        }
        Billing bill = bills.get(n - 1);

        System.out.println();
        System.out.println("Business        : " + bill.businessName());
        System.out.println("Period          : " + InputHelper.formatPeriod(bill.billingPeriod()));
        System.out.println("Total Amount Due: " + InputHelper.peso(bill.totalAmount())
                + (bill.lateFee() > 0 ? " (includes late fee " + InputHelper.peso(bill.lateFee()) + ")" : ""));
        System.out.println();

        double payment = InputHelper.readNonNegativeDouble(scanner, "Enter Payment   : ");

        // The small 0.005 allowance avoids false "insufficient" results caused by decimal rounding.
        if (payment + 0.005 >= bill.totalAmount()) {
            double change = BillingDao.round2(payment - bill.totalAmount());
            // The amount recorded as paid is the bill total, NOT the cash handed over,
            // so the change is never counted as money collected.
            if (billingDao.markPaid(bill.billingId(), bill.totalAmount())) {
                logDao.log(user.id(), "UPDATE", "Payment received for " + bill.businessName() + " ("
                        + InputHelper.formatPeriod(bill.billingPeriod()) + "): "
                        + InputHelper.peso(bill.totalAmount()));
                System.out.println();
                System.out.println("[OK] Payment Successful");
                System.out.println("Amount Due      : " + InputHelper.peso(bill.totalAmount()));
                System.out.println("Amount Entered  : " + InputHelper.peso(payment));
                System.out.println("Change          : " + InputHelper.peso(change));
                System.out.println("Payment Status  : PAID");
            } else {
                System.out.println("[!] Could not record the payment.");
            }
        } else {
            System.out.println();
            System.out.println("[!] INSUFFICIENT PAYMENT");
            System.out.println("Amount Due      : " + InputHelper.peso(bill.totalAmount()));
            System.out.println("Amount Entered  : " + InputHelper.peso(payment));
            System.out.println("Remaining       : " + InputHelper.peso(bill.totalAmount() - payment));
            System.out.println("The bill stays " + bill.paymentStatus() + ".");
        }
    }
}
