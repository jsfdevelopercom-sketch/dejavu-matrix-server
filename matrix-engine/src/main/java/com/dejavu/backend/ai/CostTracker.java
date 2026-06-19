package com.dejavu.backend.ai;

import com.dejavu.backend.model.ApiCostRecord;
import com.dejavu.backend.repository.ApiCostRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Component
public class CostTracker {

    @Autowired
    private ApiCostRecordRepository costRepository;

    private double thisRunCost = 0.0;

    @PostConstruct
    public void init() {
        System.out.println("[CostTracker] Rebooting and syncing from DB...");
        // Ensure local folder exists
        File dir = new File("api/warnings/cost");
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    public synchronized void trackCost(String provider, String model, int inputTokens, int outputTokens) {
        double inputCostPerM = 0;
        double outputCostPerM = 0;

        // Apply 2026 latest known pricing per 1M tokens
        model = model.toLowerCase();
        if (provider.equalsIgnoreCase("OPENAI")) {
            // Assumed GPT-4o base or fallback
            inputCostPerM = 2.50;
            outputCostPerM = 10.00;
        } else if (provider.equalsIgnoreCase("GEMINI")) {
            if (model.contains("pro")) {
                inputCostPerM = 1.25;
                outputCostPerM = 5.00;
            } else if (model.contains("flash")) {
                inputCostPerM = 0.35;
                outputCostPerM = 1.50;
            } else {
                inputCostPerM = 1.25; // fallback to pro
                outputCostPerM = 5.00;
            }
        } else if (provider.equalsIgnoreCase("CLAUDE")) {
            if (model.contains("opus")) {
                inputCostPerM = 5.00;
                outputCostPerM = 25.00;
            } else if (model.contains("sonnet")) {
                inputCostPerM = 3.00;
                outputCostPerM = 15.00;
            } else if (model.contains("haiku")) {
                inputCostPerM = 1.00;
                outputCostPerM = 5.00;
            }
        }

        double cost = ((inputTokens / 1_000_000.0) * inputCostPerM) + ((outputTokens / 1_000_000.0) * outputCostPerM);
        thisRunCost += cost;

        ApiCostRecord record = new ApiCostRecord();
        record.setApiProvider(provider);
        record.setModelName(model);
        record.setInputTokens(inputTokens);
        record.setOutputTokens(outputTokens);
        record.setCostIncurred(cost);
        costRepository.save(record);
        
        logLocalText("TRACK", String.format("[%s] Used %s. In: %d, Out: %d. Cost: $%.6f", provider, model, inputTokens, outputTokens, cost));
    }

    public double getThisRunCost() {
        return thisRunCost;
    }

    public double getCostLastHour() {
        return costRepository.getTotalCostSince(LocalDateTime.now().minusHours(1));
    }

    public double getCostThisDay() {
        return costRepository.getTotalCostSince(LocalDateTime.now().truncatedTo(ChronoUnit.DAYS));
    }

    public double getCostThisWeek() {
        return costRepository.getTotalCostSince(LocalDateTime.now().minusWeeks(1));
    }

    public double getCostThisMonth() {
        return costRepository.getTotalCostSince(LocalDateTime.now().minusMonths(1));
    }

    public double getTotalCost() {
        return costRepository.getTotalCostTillDate();
    }

    public String generateCostTable() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-20s | %-10s\n", "Metric", "Cost (USD)"));
        sb.append("---------------------|-----------\n");
        sb.append(String.format("%-20s | $%.4f\n", "Last Hour", getCostLastHour()));
        sb.append(String.format("%-20s | $%.4f\n", "This Run", getThisRunCost()));
        sb.append(String.format("%-20s | $%.4f\n", "Today", getCostThisDay()));
        sb.append(String.format("%-20s | $%.4f\n", "This Week", getCostThisWeek()));
        sb.append(String.format("%-20s | $%.4f\n", "This Month", getCostThisMonth()));
        sb.append(String.format("%-20s | $%.4f\n", "Total Till Date", getTotalCost()));
        return sb.toString();
    }

    @Scheduled(fixedRate = 3600000) // Every hour
    public void scheduledSyncAndLog() {
        System.out.println("[CostTracker] Running scheduled DB to local file sync...");
        String table = generateCostTable();
        logLocalText("SYNC", "Periodic DB Cost Sync:\n" + table);
    }

    public void logLocalText(String type, String message) {
        String dateStr = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd").format(LocalDateTime.now());
        File file = new File("api/warnings/cost/" + dateStr + "-log.txt");
        try (FileWriter fw = new FileWriter(file, true)) {
            String timestamp = LocalDateTime.now().toString();
            fw.write(String.format("[%s] [%s] %s\n", timestamp, type, message));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
