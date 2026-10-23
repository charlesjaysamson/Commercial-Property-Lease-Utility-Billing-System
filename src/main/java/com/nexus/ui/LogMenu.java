package com.nexus.ui;

import com.nexus.dao.ActivityLogDao;
import com.nexus.model.ActivityLog;

import java.util.List;
import java.util.Scanner;

/**
 * Administrator-only screen: shows the System Activity Log.
 */
public class LogMenu {

    private final Scanner scanner;

    public LogMenu(Scanner scanner) {
        this.scanner = scanner;
    }

    private static final int SHOW_LAST = 50;

    private final ActivityLogDao logDao = new ActivityLogDao();

    public void run() {
        InputHelper.clearScreen();
        InputHelper.printHeader("SYSTEM ACTIVITY LOG (latest " + SHOW_LAST + ")");
        System.out.println();

        List<ActivityLog> logs = logDao.findRecent(SHOW_LAST);
        if (logs.isEmpty()) {
            System.out.println("No activity recorded yet.");
        } else {
            System.out.printf("%-20s %-14s %-8s %s%n", "Date / Time", "User", "Action", "Description");
            InputHelper.printLine(100, '-');
            for (ActivityLog log : logs) {
                System.out.printf("%-20s %-14s %-8s %s%n", log.logTimestamp(),
                        InputHelper.cut(log.username(), 14), log.action(), log.description());
            }
            InputHelper.printLine(100, '-');
        }
        InputHelper.pause(scanner);
    }
}
