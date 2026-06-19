package com.dejavu.backend.ai;

import com.dejavu.backend.model.CostWarningRecord;
import com.dejavu.backend.repository.CostWarningRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CostLimiter {

    @Autowired
    private CostTracker costTracker;

    @Autowired
    private CostWarningRecordRepository warningRepository;

    private boolean isCutOff = false;

    // Defined markers (in USD)
    private static final double LOW_MARKER = 5.0;
    private static final double MID_MARKER = 20.0;
    private static final double CEILING_MARKER = 50.0; // Hard cutoff

    private boolean lowWarningTriggered = false;
    private boolean midWarningTriggered = false;
    private boolean dailyWarningTriggered = false;

    public void checkLimits() {
        if (isCutOff) return; // Already dead

        double currentRunCost = costTracker.getThisRunCost();
        double currentDayCost = costTracker.getCostThisDay();
        
        if (currentRunCost >= CEILING_MARKER && !isCutOff) {
            isCutOff = true;
            String message = "CEILING MARKER REACHED ($" + currentRunCost + "). ALL API CALLS GLOBALLY KILLED.";
            System.err.println(message);
            String table = costTracker.generateCostTable();
            costTracker.logLocalText("CUTOFF", message + "\n" + table);
            saveWarningToDb("CUTOFF", message, table);
        } else if (currentRunCost >= MID_MARKER && !midWarningTriggered) {
            midWarningTriggered = true;
            String message = "MID WARNING: Cost reached $" + currentRunCost;
            System.err.println(message);
            String table = costTracker.generateCostTable();
            costTracker.logLocalText("WARNING_MID", message + "\n" + table);
            saveWarningToDb("MID", message, table);
        } else if (currentRunCost >= LOW_MARKER && !lowWarningTriggered) {
            lowWarningTriggered = true;
            String message = "LOW WARNING: Cost reached $" + currentRunCost;
            System.err.println(message);
            String table = costTracker.generateCostTable();
            costTracker.logLocalText("WARNING_LOW", message + "\n" + table);
            saveWarningToDb("LOW", message, table);
        }

        if (currentDayCost >= LOW_MARKER && !dailyWarningTriggered) {
            dailyWarningTriggered = true;
            String message = "DAILY LOW WARNING: Cost reached $" + currentDayCost + " today.";
            System.err.println(message);
            String table = costTracker.generateCostTable();
            costTracker.logLocalText("WARNING_DAILY_LOW", message + "\n" + table);
            saveWarningToDb("DAILY_LOW", message, table);
        }
    }

    public String enforcePromptSizeLimit(String prompt, String modelTier) {
        if (prompt == null) return null;
        if (modelTier.equalsIgnoreCase("HIGH") || modelTier.equalsIgnoreCase("MID")) {
            String[] words = prompt.split("\\s+");
            if (words.length > 300) {
                System.err.println("HARD FILTER TRIGGERED: " + modelTier + " model prompt exceeded 300 words (" + words.length + "). Truncating.");
                StringBuilder truncated = new StringBuilder();
                for (int i = 0; i < 300; i++) {
                    truncated.append(words[i]).append(" ");
                }
                return truncated.toString().trim();
            }
        }
        return prompt;
    }

    private void saveWarningToDb(String level, String message, String table) {
        try {
            CostWarningRecord record = new CostWarningRecord();
            record.setWarningLevel(level);
            record.setMessage(message);
            record.setCostTableSnapshot(table);
            warningRepository.save(record);
        } catch (Exception e) {
            System.err.println("Failed to save cost warning to DB: " + e.getMessage());
        }
    }

    public boolean isApiCutOff() {
        return isCutOff;
    }
}
