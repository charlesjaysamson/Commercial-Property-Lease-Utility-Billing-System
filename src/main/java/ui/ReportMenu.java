package ui;

import dao.BillingDao;
import dao.PropertyDao;
import model.Billing;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;

/**
 * Monthly Lease & Billing Report, with CSV export.
 */
public class ReportMenu {

    private final BillingDao billingDao = new BillingDao();
    private final PropertyDao propertyDao = new PropertyDao();

    public void run() {
        InputHelper.clearScreen();
        InputHelper.printHeader("MONTHLY LEASE & BILLING REPORT");
        System.out.println();

        billingDao.markOverdueBills();

        YearMonth month = InputHelper.readMonth("Report Month (YYYY-MM, e.g. 2026-09): ");
        String period = month.toString();
        List<Billing> bills = billingDao.findByPeriod(period);

        System.out.println();
        if (bills.isEmpty()) {
            System.out.println("No bills were generated for " + InputHelper.formatPeriod(period) + ".");
            InputHelper.pause();
            return;
        }

        double totalRent = 0;
        double totalUtilities = 0;
        double totalLateFees = 0;
        double totalBilled = 0;
        double totalCollected = 0;
        double outstanding = 0;

        System.out.println("BILLS FOR " + InputHelper.formatPeriod(period).toUpperCase());
        InputHelper.printLine(100, '-');
        System.out.printf("%-18s %-10s %-12s %-12s %-10s %-13s %-8s%n",
                "Business", "Property", "Rent", "Utilities", "Late Fee", "Total Due", "Status");
        InputHelper.printLine(100, '-');
        for (Billing b : bills) {
            System.out.printf("%-18s %-10s %-12s %-12s %-10s %-13s %-8s%n",
                    InputHelper.cut(b.businessName(), 18), InputHelper.cut(b.propertyName(), 10),
                    InputHelper.peso(b.rentAmount()), InputHelper.peso(b.utilityAmount()),
                    InputHelper.peso(b.lateFee()), InputHelper.peso(b.totalAmount()), b.paymentStatus());

            totalRent += b.rentAmount();
            totalUtilities += b.utilityAmount();
            totalLateFees += b.lateFee();
            totalBilled += b.totalAmount();
            if (b.paymentStatus().equals("PAID")) {
                totalCollected += b.amountPaid();
            } else {
                outstanding += b.totalAmount();
            }
        }
        InputHelper.printLine(100, '-');

        System.out.println();
        System.out.println("FINANCIAL SUMMARY");
        InputHelper.printLine(40, '-');
        System.out.println("Total Rent               : " + InputHelper.peso(totalRent));
        System.out.println("Utility Charges          : " + InputHelper.peso(totalUtilities));
        System.out.println("Late Fees                : " + InputHelper.peso(totalLateFees));
        System.out.println("Total Billed             : " + InputHelper.peso(totalBilled));
        System.out.println("Total Collected          : " + InputHelper.peso(totalCollected));
        System.out.println("Outstanding Balance      : " + InputHelper.peso(outstanding));
        System.out.println();

        int available = propertyDao.countByStatus("AVAILABLE");
        int occupied = propertyDao.countByStatus("OCCUPIED");
        System.out.println("PROPERTY STATUS (today)");
        InputHelper.printLine(40, '-');
        System.out.println("Total Units              : " + (available + occupied));
        System.out.println("Occupied Units           : " + occupied);
        System.out.println("Available Units          : " + available);
        InputHelper.printLine(60, '=');

        System.out.println();
        if (InputHelper.askYesNo("Export this report to a CSV file? (Y/N): ")) {
            exportCsv(period, bills);
        }
        InputHelper.pause();
    }

    private void exportCsv(String period, List<Billing> bills) {
        Path folder = Paths.get("reports");
        Path file = folder.resolve("Monthly_Lease_Billing_Report_" + period + ".csv");

        try {
            Files.createDirectories(folder);
            try (BufferedWriter out = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                out.write("Business,Tenant,Property,Billing Period,Rent,Utilities,Late Fee,"
                        + "Total Due,Amount Paid,Due Date,Status");
                out.newLine();
                for (Billing b : bills) {
                    out.write(String.join(",",
                            csv(b.businessName()), csv(b.tenantName()), csv(b.propertyName()),
                            b.billingPeriod(), num(b.rentAmount()), num(b.utilityAmount()),
                            num(b.lateFee()), num(b.totalAmount()), num(b.amountPaid()),
                            InputHelper.formatDate(b.dueDate()), b.paymentStatus()));
                    out.newLine();
                }
            }
            System.out.println("[OK] Report exported to: " + file.toAbsolutePath());
        } catch (IOException e) {
            System.out.println("[!] Could not export the report: " + e.getMessage());
        }
    }

    // Wraps text in quotes when it contains a comma or quote, so the CSV columns stay correct.
    private String csv(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private String num(double value) {
        return String.format(Locale.US, "%.2f", value);
    }
}
