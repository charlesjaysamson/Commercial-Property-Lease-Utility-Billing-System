package ui;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Locale;
import java.util.Scanner;

/**
 * Shared console tools: one Scanner for the whole app, input validation,
 * and formatting for money, dates, and screen headers.
 */
public class InputHelper {

    private static final Scanner SC = new Scanner(System.in);
    private static final DecimalFormat MONEY =
            new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US));
    // STRICT makes impossible dates such as 02/30/2026 get rejected instead of "fixed".
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("MM/dd/uuuu").withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter MONTH_DISPLAY =
            DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);

    private InputHelper() {
    }

    // ------------------------------------------------------------------
    // Reading input
    // ------------------------------------------------------------------
    private static String ask(String prompt) {
        System.out.print(prompt);
        System.out.flush();
        return SC.nextLine();
    }

    /** Keeps asking until the person types something. */
    public static String readText(String prompt) {
        while (true) {
            String value = ask(prompt).trim();
            if (!value.isEmpty()) {
                return value;
            }
            System.out.println("[!] This field cannot be empty. Please try again.");
        }
    }

    /** Pressing ENTER keeps the current value. */
    public static String readTextOrKeep(String prompt, String current) {
        String value = ask(prompt).trim();
        return value.isEmpty() ? current : value;
    }

    /** Passwords are not trimmed, so spaces are allowed. */
    public static String readPassword(String prompt) {
        while (true) {
            String value = ask(prompt);
            if (!value.isEmpty()) {
                return value;
            }
            System.out.println("[!] Password cannot be empty.");
        }
    }

    private static boolean isValidEmail(String email) {
        return email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    }

    public static String readEmail(String prompt) {
        while (true) {
            String value = readText(prompt);
            if (isValidEmail(value)) {
                return value;
            }
            System.out.println("[!] Please enter a valid email address (example: name@mail.com).");
        }
    }

    public static String readEmailOrKeep(String prompt, String current) {
        while (true) {
            String value = ask(prompt).trim();
            if (value.isEmpty()) {
                return current;
            }
            if (isValidEmail(value)) {
                return value;
            }
            System.out.println("[!] Please enter a valid email address (example: name@mail.com).");
        }
    }

    public static int readInt(String prompt) {
        while (true) {
            String input = ask(prompt).trim();
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("[!] Invalid input. Please enter a number.");
            }
        }
    }

    /** Reads a number from 0 to max. 0 means cancel. */
    public static int chooseNumber(String prompt, int max) {
        while (true) {
            int n = readInt(prompt);
            if (n >= 0 && n <= max) {
                return n;
            }
            System.out.println("[!] Please enter a number from 0 to " + max + " (0 = cancel).");
        }
    }

    private static double parseMoney(String input) throws NumberFormatException {
        double value = Double.parseDouble(input);
        if (!Double.isFinite(value)) {
            throw new NumberFormatException("not finite");
        }
        return value;
    }

    public static double readNonNegativeDouble(String prompt) {
        while (true) {
            try {
                double value = parseMoney(ask(prompt).trim());
                if (value < 0) {
                    System.out.println("[!] Value cannot be negative. Please try again.");
                } else {
                    return value;
                }
            } catch (NumberFormatException e) {
                System.out.println("[!] Invalid number. Please try again.");
            }
        }
    }

    public static double readPositiveDouble(String prompt) {
        while (true) {
            double value = readNonNegativeDouble(prompt);
            if (value > 0) {
                return value;
            }
            System.out.println("[!] Value must be greater than zero.");
        }
    }

    /** Pressing ENTER returns null (meaning "keep the current value"). */
    public static Double readOptionalPositiveDouble(String prompt) {
        while (true) {
            String input = ask(prompt).trim();
            if (input.isEmpty()) {
                return null;
            }
            try {
                double value = parseMoney(input);
                if (value > 0) {
                    return value;
                }
                System.out.println("[!] Value must be greater than zero.");
            } catch (NumberFormatException e) {
                System.out.println("[!] Invalid number. Please try again.");
            }
        }
    }

    public static LocalDate readDate(String prompt) {
        while (true) {
            String input = ask(prompt).trim();
            try {
                return LocalDate.parse(input, DATE_FORMAT);
            } catch (DateTimeParseException e) {
                System.out.println("[!] Invalid date. Use MM/DD/YYYY (example: 09/30/2026).");
            }
        }
    }

    /** Pressing ENTER returns null (meaning "keep the current value"). */
    public static LocalDate readOptionalDate(String prompt) {
        while (true) {
            String input = ask(prompt).trim();
            if (input.isEmpty()) {
                return null;
            }
            try {
                return LocalDate.parse(input, DATE_FORMAT);
            } catch (DateTimeParseException e) {
                System.out.println("[!] Invalid date. Use MM/DD/YYYY (example: 09/30/2026).");
            }
        }
    }

    public static YearMonth readMonth(String prompt) {
        while (true) {
            String input = ask(prompt).trim();
            try {
                return YearMonth.parse(input);
            } catch (DateTimeParseException e) {
                System.out.println("[!] Invalid month. Use YYYY-MM (example: 2026-09).");
            }
        }
    }

    public static boolean askYesNo(String prompt) {
        while (true) {
            String input = ask(prompt).trim().toUpperCase();
            if (input.equals("Y")) {
                return true;
            } else if (input.equals("N")) {
                return false;
            }
            System.out.println("[!] Please enter Y or N.");
        }
    }

    public static void pause() {
        System.out.println();
        ask("Press ENTER to continue...");
    }

    // ------------------------------------------------------------------
    // Formatting
    // ------------------------------------------------------------------
    public static String peso(double amount) {
        return "\u20B1" + MONEY.format(amount);
    }

    public static String formatDate(LocalDate date) {
        return date.format(DATE_FORMAT);
    }

    /** "2026-09" becomes "September 2026". */
    public static String formatPeriod(String period) {
        return YearMonth.parse(period).format(MONTH_DISPLAY);
    }

    /** Shortens long text so table columns stay lined up. */
    public static String cut(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max - 1) + ".";
    }

    public static void clearScreen() {
        for (int i = 0; i < 40; i++) {
            System.out.println();
        }
    }

    public static void printLine(int length, char symbol) {
        System.out.println(String.valueOf(symbol).repeat(length));
    }

    public static void printHeader(String title) {
        printLine(60, '=');
        System.out.println("NEXUS PROPERTY MANAGEMENT");
        System.out.println("COMMERCIAL PROPERTY LEASE & UTILITY BILLING SYSTEM");
        printLine(60, '=');
        System.out.println();
        System.out.println(title);
        printLine(title.length(), '=');
    }
}
