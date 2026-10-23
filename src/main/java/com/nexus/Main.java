package com.nexus;

import com.nexus.dao.DatabaseInitializer;
import com.nexus.dao.UserDao;
import com.nexus.ui.Authentication;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * NEXUS PROPERTY MANAGEMENT
 * Commercial Property Lease & Utility Billing System
 *
 * The ONLY entry point. It does exactly three things, in this order:
 *   1. Initialize the database
 *   2. Create the shared DAO and utilities (ONE UserDao, ONE Scanner)
 *   3. Hand control to Authentication.start()
 */
public class Main {

    public static void main(String[] args) {
        // Force UTF-8 output so the peso sign displays correctly.
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));

        // 1. Initialize the database. If this fails, the app does not start.
        try {
            DatabaseInitializer.initialize();
        } catch (IllegalStateException e) {
            System.out.println(e.getMessage());
            return;
        }

        // 2. Instantiate the DAO and utilities. These are shared with every menu.
        Scanner scanner = new Scanner(System.in);
        UserDao userDao = new UserDao();

        // 3. Delegate everything else to Authentication.
        try {
            new Authentication(userDao, scanner).start();
        } catch (NoSuchElementException e) {
            // Input stream was closed (for example Ctrl+D), so exit quietly.
            System.out.println();
            System.out.println("Input closed. Exiting.");
        } finally {
            scanner.close();
        }
    }
}
