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

    public synchronized void checkLimits() {
        if (isCutOff) return; // Already dead

        double currentRunCost = costTracker.getThisRunCost();
        
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
