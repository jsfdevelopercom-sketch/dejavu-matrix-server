package com.dejavu.backend.ai.agent;

import com.dejavu.backend.ai.OpenAiClient;
import com.dejavu.backend.ai.GeminiAiClient;

import com.dejavu.backend.ai.MemoryCondenser;
import com.dejavu.backend.ai.AiOutputJudge;

public class AgentMind {

    private final OpenAiClient openAiClient;
    private final GeminiAiClient geminiAiClient;
    private final MemoryCondenser memoryCondenser;
    private final AiOutputJudge outputJudge;
    
    // The internal state
    private String longTermMemory;
    private String shortTermMemory;
    private String personality;
    private String relations;
    private String eventLogs;
    private final com.dejavu.backend.ai.PersonalityEngine personalityEngine;
    private final com.dejavu.backend.ai.RelationsEngine relationsEngine;

    public AgentMind(OpenAiClient openAiClient, GeminiAiClient geminiAiClient, MemoryCondenser memoryCondenser, AiOutputJudge outputJudge, com.dejavu.backend.ai.PersonalityEngine personalityEngine, com.dejavu.backend.ai.RelationsEngine relationsEngine, String personality, String relations, String ltm, String stm, String eventLogs) {
        this.openAiClient = openAiClient;
        this.geminiAiClient = geminiAiClient;
        this.memoryCondenser = memoryCondenser;
        this.outputJudge = outputJudge;
        this.personalityEngine = personalityEngine;
        this.relationsEngine = relationsEngine;
        this.personality = personality;
        this.relations = relations;
        this.longTermMemory = ltm == null ? "" : ltm;
        this.shortTermMemory = stm == null ? "" : stm;
        this.eventLogs = eventLogs == null ? "" : eventLogs;
    }

    public String think(String context, String prompt) {
        String condensedMemory = memoryCondenser.condense(longTermMemory, shortTermMemory);
        String systemPrompt = "You are a human mind in the Matrix. Personality: " + personality + "\nCondensed Memory: " + condensedMemory + "\nContext: " + context + "\n\nCRITICAL RULE: If you are overwhelmed, in danger, or need absolute highest-tier help from a God-like operator, you can call for 'Ramon'. To do this, include the EXACT phrase <CALL_RAMON> in your thoughts. Doing so will immediately alert the operator to jump into your mind.";
        String combinedInstruction = systemPrompt + "\n\n" + prompt;
        
        // 1. Try Low Model
        String result = geminiAiClient.generateContentLight(combinedInstruction);
        if (outputJudge.isOutputAdequate(combinedInstruction, result)) {
            return result;
        }
        
        System.out.println("Low model output deemed inadequate or failed. Escalating to mid model...");
        // 2. Escalation: Try Mid Model (Fallback to heavy if mid unsupported natively here)
        result = geminiAiClient.generateContentHeavy(combinedInstruction); 
        if (outputJudge.isOutputAdequate(combinedInstruction, result)) {
            return result;
        }
        
        System.out.println("Mid model output deemed inadequate or failed. Escalating to HIGH model...");
        // 3. Escalation: Try High Model
        return openAiClient.generateContent(systemPrompt, prompt);
    }

    public String thinkHeavy(String context, String prompt) {
        String condensedMemory = memoryCondenser.condense(longTermMemory, shortTermMemory);
        String systemPrompt = "You are a human mind in the Matrix. Personality: " + personality + "\nCondensed Memory: " + condensedMemory + "\nContext: " + context + "\n\nCRITICAL RULE: If you are overwhelmed, in danger, or need absolute highest-tier help from a God-like operator, you can call for 'Ramon'. To do this, include the EXACT phrase <CALL_RAMON> in your thoughts. Doing so will immediately alert the operator to jump into your mind.";
        return openAiClient.generateContent(systemPrompt, prompt);
    }

    public String processEvent(String event) {
        String prompt = "Absorb this event and output your internal conscious stream of thoughts about it: " + event;
        String thoughts = think("Processing a new event.", prompt);
        if (thoughts == null) thoughts = "I processed the event but my thoughts are clouded.";
        
        java.time.ZonedDateTime ncrTime = java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Kolkata"));
        String timeStr = ncrTime.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a 'NCR Time'"));
        String stampedEvent = "[" + timeStr + "] " + thoughts;
        
        this.shortTermMemory = stampedEvent + "\n\n" + this.shortTermMemory;
        if (this.shortTermMemory.length() > 5000) {
            this.shortTermMemory = this.shortTermMemory.substring(0, 5000);
        }
        
        extractCoreMemories();
        return thoughts;
    }

    private void extractCoreMemories() {
        String prompt = "Review this STM:\n" + shortTermMemory + "\n\nIs there a highly significant event (very good, bad, scary, novel, important)? If YES, output exactly ONE sentence summarizing it to permanent memory starting with exactly one tag: [POSITIVE], [NEGATIVE], or [NEUTRAL]. If NO, output 'NONE'.";
        String ltmExtracted = geminiAiClient.generateContentLight(prompt);
        if (ltmExtracted == null || ltmExtracted.contains("[GEMINI_ERROR]")) ltmExtracted = openAiClient.generateContent("You are a memory extractor.", prompt);
        
        if (ltmExtracted != null && !ltmExtracted.contains("NONE") && ltmExtracted.length() > 5) {
            String color = "#c8c8c8"; // neutral
            if (ltmExtracted.startsWith("[POSITIVE]")) {
                color = "#4caf50"; // green
                ltmExtracted = ltmExtracted.replace("[POSITIVE]", "").trim();
            } else if (ltmExtracted.startsWith("[NEGATIVE]")) {
                color = "#f44336"; // red
                ltmExtracted = ltmExtracted.replace("[NEGATIVE]", "").trim();
            } else if (ltmExtracted.startsWith("[NEUTRAL]")) {
                ltmExtracted = ltmExtracted.replace("[NEUTRAL]", "").trim();
            }

            java.time.ZonedDateTime ncrTime = java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Kolkata"));
            String timeStr = ncrTime.format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy"));

            this.longTermMemory = "<span style='color:" + color + "'>[CORE MEMORY: " + timeStr + "] " + ltmExtracted.trim() + "</span><br>\n" + this.longTermMemory;
            this.shortTermMemory = "Mind is clear."; // Flush STM to make room
            
            // Amend personality and relations based on this core memory
            this.personality = personalityEngine.amendPersonality(this.personality, ltmExtracted);
            this.relations = relationsEngine.amendRelations(this.relations, ltmExtracted);
        }
    }

    public String getLongTermMemory() {
        return longTermMemory;
    }

    public String getShortTermMemory() {
        return shortTermMemory;
    }

    public void logEvent(String event) {
        if (this.eventLogs == null) this.eventLogs = "";
        java.time.ZonedDateTime ncrTime = java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Kolkata"));
        String timeStr = ncrTime.format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a"));
        // Color code event logs
        String color = "#b3e5fc"; // Light blue for generic events
        if (event.contains("REJECTED") || event.contains("failed") || event.contains("ERROR")) color = "#f44336";
        this.eventLogs = "<span style='color:" + color + "'>[" + timeStr + "] " + event + "</span><br>\n" + this.eventLogs;
    }

    public String getPersonality() {
        return personality;
    }

    public String getRelations() {
        return relations;
    }

    public String getEventLogs() {
        return eventLogs;
    }
}
