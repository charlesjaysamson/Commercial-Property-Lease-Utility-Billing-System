package ui;

import dao.ActivityLogDao;
import dao.PropertyDao;
import model.Property;
import model.User;

import java.util.List;

/**
 * Property / unit management. "Delete" is a soft-delete: status becomes INACTIVE.
 */
public class PropertyMenu {

    private final User user;
    private final PropertyDao propertyDao = new PropertyDao();
    private final ActivityLogDao logDao = new ActivityLogDao();

    public PropertyMenu(User user) {
        this.user = user;
    }

    public void run() {
        boolean back = false;
        while (!back) {
            InputHelper.clearScreen();
            InputHelper.printHeader("PROPERTY / UNIT MANAGEMENT");
            System.out.println();
            System.out.println("[1] View Properties");
            System.out.println("[2] Add Property");
            System.out.println("[3] Search Properties");
            System.out.println("[4] Deactivate Property (Soft-Delete)");
            System.out.println("[5] Reactivate Property");
            System.out.println("[0] Back");
            InputHelper.printLine(60, '-');

            switch (InputHelper.readInt("Enter choice: ")) {
                case 1 -> {
                    showTable(propertyDao.findAll());
                    InputHelper.pause();
                }
                case 2 -> {
                    addProperty();
                    InputHelper.pause();
                }
                case 3 -> {
                    searchProperties();
                    InputHelper.pause();
                }
                case 4 -> {
                    changeStatus("AVAILABLE", "INACTIVE", "Deactivate");
                    InputHelper.pause();
                }
                case 5 -> {
                    changeStatus("INACTIVE", "AVAILABLE", "Reactivate");
                    InputHelper.pause();
                }
                case 0 -> back = true;
                default -> {
                    System.out.println("[!] Invalid menu choice.");
                    InputHelper.pause();
                }
            }
        }
    }

    private void showTable(List<Property> list) {
        System.out.println();
        if (list.isEmpty()) {
            System.out.println("No properties found.");
            return;
        }
        System.out.printf("%-4s %-18s %-20s %-16s %-10s%n", "No.", "Property", "Location", "Monthly Rent", "Status");
        InputHelper.printLine(72, '-');
        for (int i = 0; i < list.size(); i++) {
            Property p = list.get(i);
            System.out.printf("%-4d %-18s %-20s %-16s %-10s%n", i + 1,
                    InputHelper.cut(p.propertyName(), 18), InputHelper.cut(p.location(), 20),
                    InputHelper.peso(p.rentalRate()), p.status());
        }
        InputHelper.printLine(72, '-');
    }

    private void addProperty() {
        System.out.println();
        System.out.println("ADD NEW PROPERTY");
        InputHelper.printLine(20, '-');

        String name = InputHelper.readText("Property Name (e.g. Unit A01) : ");
        if (propertyDao.existsByName(name)) {
            System.out.println("[!] A property with that name already exists. Nothing was added.");
            return;
        }
        String location = InputHelper.readText("Location (e.g. Ground Floor)  : ");
        double rate = InputHelper.readPositiveDouble("Monthly Rental Rate           : ");

        if (propertyDao.add(name, location, rate)) {
            logDao.log(user.id(), "CREATE", "Added property " + name + " (" + InputHelper.peso(rate) + "/month)");
            System.out.println();
            System.out.println("[OK] Property " + name + " has been added (status: AVAILABLE).");
        } else {
            System.out.println("[!] Could not add the property.");
        }
    }

    private void searchProperties() {
        System.out.println();
        String keyword = InputHelper.readText("Search (name or location): ");
        showTable(propertyDao.search(keyword));
    }

    // Soft-delete / reactivate: only the status column changes, the row is kept.
    private void changeStatus(String fromStatus, String toStatus, String verb) {
        System.out.println();
        List<Property> list = propertyDao.findByStatus(fromStatus);
        if (list.isEmpty()) {
            System.out.println("No " + fromStatus + " properties to " + verb.toLowerCase() + ".");
            return;
        }
        showTable(list);
        int n = InputHelper.chooseNumber("Enter property number to " + verb.toLowerCase() + " (0 to cancel): ", list.size());
        if (n == 0) {
            System.out.println("[!] Cancelled.");
            return;
        }
        Property p = list.get(n - 1);
        if (!InputHelper.askYesNo(verb + " " + p.propertyName() + "? (Y/N): ")) {
            System.out.println("[!] Cancelled.");
            return;
        }
        if (propertyDao.updateStatus(p.propertyId(), toStatus)) {
            String action = toStatus.equals("INACTIVE") ? "DELETE" : "UPDATE";
            logDao.log(user.id(), action, "Property " + p.propertyName() + " status changed to " + toStatus);
            System.out.println("[OK] " + p.propertyName() + " is now " + toStatus + ".");
        } else {
            System.out.println("[!] Could not update the property.");
        }
    }
}
