package com.dejavu.backend.ai.agent;

import com.dejavu.backend.ai.OpenAiClient;
import com.dejavu.backend.ai.GeminiAiClient;

public class AgentMind {

    private final OpenAiClient openAiClient;
    private final GeminiAiClient geminiAiClient;
    
    // The internal state
    private String longTermMemory;
    private String shortTermMemory;
    private String personality;

    public AgentMind(OpenAiClient openAiClient, GeminiAiClient geminiAiClient, String personality, String ltm, String stm) {
        this.openAiClient = openAiClient;
        this.geminiAiClient = geminiAiClient;
        this.personality = personality;
        this.longTermMemory = ltm == null ? "" : ltm;
        this.shortTermMemory = stm == null ? "" : stm;
    }

    public String think(String context, String prompt) {
        String systemPrompt = "You are a human mind in the Matrix. Personality: " + personality + "\nLTM: " + longTermMemory + "\nSTM: " + shortTermMemory + "\nContext: " + context;
        return openAiClient.generateContent(systemPrompt, prompt);
    }

    public String processEvent(String event) {
        String prompt = "Absorb this event and output your internal conscious stream of thoughts about it: " + event;
        String thoughts = think("Processing a new event.", prompt);
        
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
        String prompt = "Review this STM:\n" + shortTermMemory + "\n\nIs there a highly significant event (very good, bad, scary, novel, important)? If YES, output exactly ONE sentence summarizing it to permanent memory. If NO, output 'NONE'.";
        String ltmExtracted = openAiClient.generateContent(prompt);
        if (ltmExtracted != null && !ltmExtracted.contains("NONE") && ltmExtracted.length() > 5) {
            this.longTermMemory = "[CORE MEMORY]: " + ltmExtracted.trim() + "\n" + this.longTermMemory;
            this.shortTermMemory = ""; // Flush STM to make room
        }
    }

    public String getLongTermMemory() {
        return longTermMemory;
    }

    public String getShortTermMemory() {
        return shortTermMemory;
    }
}
