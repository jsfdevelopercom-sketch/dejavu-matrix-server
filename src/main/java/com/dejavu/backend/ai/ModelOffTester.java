package com.dejavu.backend.ai;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ModelOffTester {

    @Autowired
    private ClaudeAiClient claudeAiClient;

    @Autowired
    private GeminiAiClient geminiAiClient;

    @Autowired
    private OpenAiClient openAiClient;

    public String testHighModelsOff() {
        boolean originalHigh = ClaudeAiClient.HIGH_MODELS_ENABLED;
        ClaudeAiClient.HIGH_MODELS_ENABLED = false;
        GeminiAiClient.HIGH_MODELS_ENABLED = false;
        OpenAiClient.HIGH_MODELS_ENABLED = false;

        StringBuilder sb = new StringBuilder();
        
        String claudeRes = claudeAiClient.generateContentHeavy("Test", false);
        if (claudeRes != null && claudeRes.contains("HIGH_MODELS_DISABLED")) {
            sb.append("Claude Heavy: CONFIRMED OFF. ");
        } else {
            sb.append("Claude Heavy: FAILED TO TURN OFF. ");
        }

        String geminiRes = geminiAiClient.generateContentHeavy("Test", false);
        if (geminiRes != null && geminiRes.contains("HIGH_MODELS_DISABLED")) {
            sb.append("Gemini Heavy: CONFIRMED OFF. ");
        } else {
            sb.append("Gemini Heavy: FAILED TO TURN OFF. ");
        }

        String openAiRes = openAiClient.generateContent("Test");
        if (openAiRes != null && openAiRes.contains("HIGH_MODELS_DISABLED")) {
            sb.append("OpenAI: CONFIRMED OFF.");
        } else {
            sb.append("OpenAI: FAILED TO TURN OFF.");
        }

        return sb.toString().trim();
    }

    public String testAllModelsOff() {
        boolean originalAll = ClaudeAiClient.ALL_MODELS_ENABLED;
        ClaudeAiClient.ALL_MODELS_ENABLED = false;
        GeminiAiClient.ALL_MODELS_ENABLED = false;
        OpenAiClient.ALL_MODELS_ENABLED = false;

        StringBuilder sb = new StringBuilder();
        
        String claudeRes = claudeAiClient.generateContentLight("Test", false);
        if (claudeRes != null && claudeRes.contains("ALL_MODELS_DISABLED")) {
            sb.append("Claude All: CONFIRMED OFF. ");
        } else {
            sb.append("Claude All: FAILED TO TURN OFF. ");
        }

        String geminiRes = geminiAiClient.generateContentLight("Test", false);
        if (geminiRes != null && geminiRes.contains("ALL_MODELS_DISABLED")) {
            sb.append("Gemini All: CONFIRMED OFF. ");
        } else {
            sb.append("Gemini All: FAILED TO TURN OFF. ");
        }

        String openAiRes = openAiClient.generateContent("Test");
        if (openAiRes != null && openAiRes.contains("ALL_MODELS_DISABLED")) {
            sb.append("OpenAI All: CONFIRMED OFF.");
        } else {
            sb.append("OpenAI All: FAILED TO TURN OFF.");
        }

        return sb.toString().trim();
    }
}
