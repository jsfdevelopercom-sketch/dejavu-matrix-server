package com.dejavu.backend.ai;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PersonalityEngine {

    @Autowired
    private ClaudeAiClient claudeAiClient;

    @Autowired
    private GeminiAiClient geminiAiClient;

    public String amendPersonality(String corePersonality, String recentMemoryDump) {
        String instruction = "You are a psychological drift engine. A human's core personality is evolving slightly based on recent events.\n" +
                "CORE PERSONALITY: " + corePersonality + "\n" +
                "RECENT MEMORY: " + recentMemoryDump + "\n\n" +
                "TASK: Return an updated personality profile. Keep the core traits intact, but subtly alter the tone, add new recent quirks, fears, or hopes based ONLY on the recent memory. Do not completely rewrite it, just append or modify slightly. Return ONLY the new personality text.";

        String updated = claudeAiClient.generateContentLight(instruction);
        if (updated == null || updated.contains("[CLAUDE_ERROR]")) {
            updated = geminiAiClient.generateContentLight(instruction);
        }
        
        if (updated != null && !updated.contains("[GEMINI_ERROR]") && !updated.contains("[CLAUDE_ERROR]")) {
            return updated;
        }
        return corePersonality;
    }
}
