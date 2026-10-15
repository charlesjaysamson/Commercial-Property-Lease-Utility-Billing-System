import dao.DatabaseConnection;
import ui.MainMenu;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;

/**
 * NEXUS PROPERTY MANAGEMENT
 * Commercial Property Lease & Utility Billing System
 *
 * App entry point. Layers:
 *   ui    -> menus the user sees
 *   dao   -> database connection and SQL (SQLite)
 *   model -> Java Records that hold the data
 */
public class Main {

    public static void main(String[] args) {
        // Force UTF-8 output so the peso sign displays correctly.
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));

        try {
            DatabaseConnection.initializeDatabase();   // creates the tables on first run
            new MainMenu().run();
        } catch (IllegalStateException e) {
            System.out.println(e.getMessage());
        } catch (NoSuchElementException e) {
            // Input stream was closed (for example Ctrl+D), so exit quietly.
            System.out.println();
            System.out.println("Input closed. Exiting.");
        }
    }
}
