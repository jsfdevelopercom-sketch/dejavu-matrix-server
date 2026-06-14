package com.dejavu.backend.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ActivityLogger {
    private static final String LOG_DIR = "user_activity_logs";

    public static void log(String username, String action) {
        try {
            File dir = new File(LOG_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            
            // Clean filename
            String cleanName = username.replaceAll("[^a-zA-Z0-9.-]", "_");
            File logFile = new File(dir, cleanName + ".log");
            
            try (PrintWriter out = new PrintWriter(new FileWriter(logFile, true))) {
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                out.println("[" + timestamp + "] " + action);
            }
        } catch (IOException e) {
            System.err.println("Failed to write to user log: " + e.getMessage());
        }
    }
}
