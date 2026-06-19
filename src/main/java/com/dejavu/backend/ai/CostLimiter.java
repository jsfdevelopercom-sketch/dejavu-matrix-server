package com.dejavu.backend.ai;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CostLimiter {

    @Autowired
    private CostTracker costTracker;

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
            costTracker.logLocalText("CUTOFF", message + "\n" + costTracker.generateCostTable());
        } else if (currentRunCost >= MID_MARKER && !midWarningTriggered) {
            midWarningTriggered = true;
            String message = "MID WARNING: Cost reached $" + currentRunCost;
            System.err.println(message);
            costTracker.logLocalText("WARNING_MID", message + "\n" + costTracker.generateCostTable());
        } else if (currentRunCost >= LOW_MARKER && !lowWarningTriggered) {
            lowWarningTriggered = true;
            String message = "LOW WARNING: Cost reached $" + currentRunCost;
            System.err.println(message);
            costTracker.logLocalText("WARNING_LOW", message + "\n" + costTracker.generateCostTable());
        }
    }

    public boolean isApiCutOff() {
        return isCutOff;
    }
}
