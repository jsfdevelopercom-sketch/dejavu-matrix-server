package com.dejavu.backend.ai.agent;

import com.dejavu.backend.ai.OpenAiClient;
import com.dejavu.backend.ai.GeminiAiClient;
import com.dejavu.backend.ai.ClaudeAiClient;
import com.dejavu.backend.ai.MemoryCondenser;
import com.dejavu.backend.ai.AiOutputJudge;

public class AgentMind {

    private final OpenAiClient openAiClient;
    private final GeminiAiClient geminiAiClient;
    private final ClaudeAiClient claudeAiClient;
    private final MemoryCondenser memoryCondenser;
    private final AiOutputJudge outputJudge;
    
    // Legacy support fields for syncToDatabaseEntity
    private String rawPersonality;
    private String rawRelations;
    private String eventLogs;
    private String longTermMemory;
    private String shortTermMemory;
    
    // V2 Architecture Components
    private final AgentState state;
    private final SalienceEngine salienceEngine;
    private final MetacognitiveRouter router;
    public final LlmGateway gateway; // Made public for easy toggle access
    private final SleepConsolidator sleepConsolidator;

    public AgentMind(OpenAiClient openAiClient, GeminiAiClient geminiAiClient, ClaudeAiClient claudeAiClient, MemoryCondenser memoryCondenser, AiOutputJudge outputJudge, com.dejavu.backend.ai.PersonalityEngine personalityEngine, com.dejavu.backend.ai.RelationsEngine relationsEngine, String personality, String relations, String ltm, String stm, String eventLogs) {
        this.openAiClient = openAiClient;
        this.geminiAiClient = geminiAiClient;
        this.claudeAiClient = claudeAiClient;
        this.memoryCondenser = memoryCondenser;
        this.outputJudge = outputJudge;
        
        this.rawPersonality = personality == null ? "" : personality;
        this.rawRelations = relations == null ? "" : relations;
        this.longTermMemory = ltm == null ? "" : ltm;
        this.shortTermMemory = stm == null ? "" : stm;
        this.eventLogs = eventLogs == null ? "" : eventLogs;
        
        this.state = new AgentState();
        this.state.memory.semanticIdentity.stableTraits.add(this.rawPersonality);
        this.state.memory.addWorkingMemory(new WorkingMemoryItem("init", this.shortTermMemory, 0.5, 10));
        
        this.salienceEngine = new SalienceEngine();
        this.router = new MetacognitiveRouter();
        this.gateway = new LlmGateway(openAiClient, geminiAiClient, claudeAiClient);
        
        // LOW MODEL SWITCH (MOE MODE ENFORCED)
        this.gateway.useLowModelOnly = true; 
        
        this.sleepConsolidator = new SleepConsolidator();
    }

    public String think(String context, String prompt) {
        return processInternal(context + " | " + prompt, false, true);
    }

    public String thinkHeavy(String context, String prompt) {
        return processInternal(context + " | " + prompt, true, true);
    }

    public String processEvent(String event) {
        return processInternal(event, false, false);
    }
    
    private String processInternal(String rawInput, boolean forceHeavy, boolean forceLlm) {
        // 1. Perception
        PerceptionFrame frame = new PerceptionFrame(rawInput, "external");
        
        // 2. Metacognition & Salience
        router.evaluate(frame, salienceEngine, state.body, state.selfModel);
        
        // 3. Update memory & goals
        state.memory.addWorkingMemory(new WorkingMemoryItem("event_" + System.currentTimeMillis(), rawInput, router.uncertainty, 5));
        state.goalEngine.evaluateGoals(state.body);
        
        // 4. Processing Path Arbitrator (Action Selection)
        AgentAction action;
        if (!router.llmNeeded && !forceHeavy && !forceLlm) {
            // Habitual / Reflex Response
            action = new AgentAction("habit", "I acknowledge the event.");
        } else {
            // Deliberative Reasoning Engine
            boolean previousMoe = gateway.useLowModelOnly;
            if (forceHeavy) gateway.useLowModelOnly = false;
            
            // Gateway mutates the state directly based on LLM output delta
            action = gateway.reason(state, frame, router);
            
            gateway.useLowModelOnly = previousMoe; // Restore
        }
        
        // 5. Autonomic Decay & Consolidation Tick
        state.body.decay(1);
        state.memory.tick();
        
        // 6. Legacy Sync (Update Strings for DB)
        this.shortTermMemory = action.content + "\n" + this.shortTermMemory;
        if (this.shortTermMemory.length() > 2000) this.shortTermMemory = this.shortTermMemory.substring(0, 2000);
        
        extractCoreMemories();
        return action.content;
    }

    private void extractCoreMemories() {
        if (shortTermMemory.length() > 500 && Math.random() > 0.8) {
            java.time.ZonedDateTime ncrTime = java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Kolkata"));
            String timeStr = ncrTime.format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy"));
            this.longTermMemory = "<span style='color:#c8c8c8'>[CORE MEMORY: " + timeStr + "] " + this.shortTermMemory.substring(0, Math.min(100, this.shortTermMemory.length())) + "</span><br>\n" + this.longTermMemory;
            this.shortTermMemory = "Mind clear.";
        }
    }

    public void logEvent(String event) {
        if (this.eventLogs == null) this.eventLogs = "";
        java.time.ZonedDateTime ncrTime = java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Kolkata"));
        String timeStr = ncrTime.format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a"));
        String color = "#b3e5fc";
        if (event.contains("REJECTED") || event.contains("failed") || event.contains("ERROR")) color = "#f44336";
        this.eventLogs = "<span style='color:" + color + "'>[" + timeStr + "] " + event + "</span><br>\n" + this.eventLogs;
    }

    public String getLongTermMemory() { return longTermMemory; }
    public String getShortTermMemory() { return shortTermMemory; }
    public String getPersonality() { return rawPersonality; }
    public String getRelations() { return rawRelations; }
    public String getEventLogs() { return eventLogs; }
    public AgentState getState() { return state; }
}
