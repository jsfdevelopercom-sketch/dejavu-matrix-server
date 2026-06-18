package com.dejavu.backend.config;

import com.dejavu.backend.ai.JuicyConfessionEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.logging.Logger;

@Component
public class ScheduledTasks {

    private static final Logger logger = Logger.getLogger(ScheduledTasks.class.getName());

    @Autowired
    private JuicyConfessionEngine juicyConfessionEngine;

    @Autowired
    private com.dejavu.backend.ai.MatrixEngine matrixEngine;

    private boolean autoEnabled = false;

    public boolean isAutoEnabled() { return autoEnabled; }
    public void setAutoEnabled(boolean autoEnabled) { this.autoEnabled = autoEnabled; }

    @Scheduled(cron = "0 0 2 * * ?")
    public void generateDailyConfessions() {
        if (!autoEnabled) {
            logger.info("Daily generator skipped because it is DISABLED.");
            return;
        }
        logger.info("Starting daily Juicy Confession Generation task...");
        
        String[] themes = {
            "Horror and Paranormal Encounters",
            "Tragic Love and Heartbreak",
            "Jealousy and Workplace Betrayal",
            "School Pranks Gone Wrong",
            "Dark Family Secrets",
            "Financial Ruin and Greed",
            "Mid-life Crisis and Regret",
            "Accidental Crimes",
            "Toxic Friendships",
            "Dangerous Obsessions"
        };
        
        int totalCreated = 0;
        
        for (String theme : themes) {
            int themeCreated = 0;
            int maxAttempts = 5;
            int attempts = 0;
            
            while (themeCreated < 3 && attempts < maxAttempts) {
                attempts++;
                int needed = 3 - themeCreated;
                logger.info("Theme [" + theme + "] Attempt " + attempts + ": Requesting " + needed + " confessions...");
                List<com.dejavu.backend.model.Confession> generated = juicyConfessionEngine.generateJuicyConfessions(theme, needed, null);
                themeCreated += generated.size();
                totalCreated += generated.size();
            }
        }
        
        logger.info("Completed daily task. Successfully created " + totalCreated + " confessions across 10 themes.");
    }

    @Scheduled(cron = "0 0 3 * * ?")
    public void runMatrixSimulation() {
        if (!autoEnabled) {
            logger.info("Matrix simulation skipped because auto is DISABLED.");
            return;
        }
        logger.info("Starting Daily Matrix Simulation. Awakening all humans...");
        // This will simulate 10 minutes of intense routine = 1 day
        matrixEngine.awakenMatrix();
        logger.info("Matrix Simulation Complete.");
    }
}
