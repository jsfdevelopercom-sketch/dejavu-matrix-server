package com.dejavu.backend.ai;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RelationsEngine {

    @Autowired
    private ClaudeAiClient claudeAiClient;

    @Autowired
    private GeminiAiClient geminiAiClient;

    public String amendRelations(String currentRelations, String recentEventOrMemory) {
        String instruction = "You are a relationship tracking engine. A human's interpersonal relationships are evolving based on a recent event.\n" +
                "CURRENT RELATIONS: " + currentRelations + "\n" +
                "RECENT EVENT/MEMORY: " + recentEventOrMemory + "\n\n" +
                "TASK: Return an updated relations profile. Add new relations if new people were introduced. Modify existing relations (make them more positive or negative) if the event affected them. Keep it concise. Return ONLY the new relations text.";

        String updated = claudeAiClient.generateContentLight(instruction);
        if (updated == null || updated.contains("[CLAUDE_ERROR]")) {
            updated = geminiAiClient.generateContentLight(instruction);
        }
        
        if (updated != null && !updated.contains("[GEMINI_ERROR]") && !updated.contains("[CLAUDE_ERROR]")) {
            return updated;
        }
        return currentRelations;
    }
}
