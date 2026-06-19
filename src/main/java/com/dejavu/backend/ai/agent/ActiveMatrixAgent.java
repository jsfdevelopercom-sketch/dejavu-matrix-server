package com.dejavu.backend.ai.agent;

import com.dejavu.backend.ai.OpenAiClient;
import com.dejavu.backend.ai.GeminiAiClient;
import com.dejavu.backend.ai.ClaudeAiClient;
import com.dejavu.backend.model.MatrixHuman;
import com.dejavu.backend.ai.MemoryCondenser;
import com.dejavu.backend.ai.AiOutputJudge;
import com.dejavu.backend.ai.PersonalityEngine;
import com.dejavu.backend.ai.RelationsEngine;

public class ActiveMatrixAgent {

    private final MatrixHuman entity;
    private final AgentMind mind;
    private final OpenAiClient openAiClient;

    public ActiveMatrixAgent(MatrixHuman entity, OpenAiClient openAiClient, GeminiAiClient geminiAiClient, ClaudeAiClient claudeAiClient, MemoryCondenser memoryCondenser, AiOutputJudge outputJudge, PersonalityEngine personalityEngine, RelationsEngine relationsEngine) {
        this.entity = entity;
        this.openAiClient = openAiClient;
        this.mind = new AgentMind(openAiClient, geminiAiClient, claudeAiClient, memoryCondenser, outputJudge, personalityEngine, relationsEngine, entity.getPersonality(), entity.getRelations(), entity.getMemory(), entity.getWorkingMemory(), entity.getEventLogs());
    }

    public String speak(String targetName, String context) {
        String prompt = "You are speaking to " + targetName + ". Context: " + context + "\nRespond naturally as yourself. Emulate text messaging style and optionally include emojis/emoticons to express your feelings towards the user.";
        return mind.think("Social Interaction", prompt);
    }

    public String ponder(String thoughtOrEvent) {
        String instruction = "\n[SYSTEM INSTRUCTION]: You possess a smartphone. If this event makes you feel a strong urge to call another specific human you know, you MUST append `<CALL:FirstName LastName>` to your thoughts.";
        return mind.think("Pondering", thoughtOrEvent + instruction);
    }

    public String experienceDay(String worldNews) {
        entity.setCurrentDay(entity.getCurrentDay() + 1);
        
        String prompt = "Simulate a day in your life. Run your routine for 10 simulated minutes which equals an entire day. Produce a concise narrative of the events of your day. " + worldNews + " CRITICAL RULE: DO NOT hallucinate interactions with other specific named characters in the Matrix. You must strictly focus on your own solo activities, your job, generic strangers, or your own internal thoughts. Any interaction with another specific named character will ONLY occur if initiated via a system-level Phone Call or World Event. Never invent a reality where you hung out with a specific person unless they are explicitly mentioned in your Event Logs.";
        String systemPrompt = "You are " + entity.getName() + ".\nOccupation: " + entity.getOccupation() + "\nAge: " + entity.getAge() + "\nPersonality: " + entity.getPersonality();
        String rawDayEvents = mind.think("Daily Simulation", systemPrompt + "\n\n" + prompt);
        
        if (rawDayEvents != null) {
            String thoughts = ponder("Day " + entity.getCurrentDay() + " Events: " + rawDayEvents);
            logEvent("Day " + entity.getCurrentDay() + " [Internal Mind State]:\n" + thoughts.trim());
            return thoughts;
        }
        return "";
    }

    public String receiveCall(ActiveMatrixAgent caller) {
        String acceptPrompt = "Incoming call from " + caller.getName() + ". Based on your past experiences with them, do you ACCEPT or REJECT? Reply with exactly one word: ACCEPT or REJECT.";
        String decision = mind.think("Phone Call Decision", acceptPrompt);
        
        if (decision != null && decision.contains("REJECT")) {
            this.logEvent("[CALL REJECTED/CUT] Rejected an incoming call from " + caller.getName() + ".");
            caller.logEvent("[CALL REJECTED/CUT] Tried calling " + this.getName() + " but they rejected/cut the call.");
            return this.getName() + " rejected the call.";
        }

        String intentPrompt = "Analyze these humans and define the secret intention of a phone call today. 1 sentence.\nCaller: " + caller.getName() + " | Receiver: " + this.getName();
        String callTheme = mind.think("Intent Analysis", intentPrompt);
        if (callTheme == null) callTheme = "Casual catch-up.";

        String transcriptPrompt = "Write a realistic transcript of a phone call where " + caller.getName() + " is calling " + this.getName() + ". " +
                "They are discussing: " + callTheme + "\n" +
                "You MUST use their EXACT names. The transcript format MUST alternate exactly like this:\n" +
                caller.getName() + ": [dialogue]\n" +
                this.getName() + ": [dialogue]\n\n" +
                "Caller Mind (" + caller.getName() + "):\n" + caller.getMindState() + "\n" +
                "Receiver Mind (" + this.getName() + "):\n" + this.getMindState();
                
        String transcript = mind.think("Transcript Generation", transcriptPrompt);
        
        if (transcript != null) {
            caller.ponder("Phone call with " + this.getName() + " (Theme: " + callTheme + "):\n" + transcript);
            this.ponder("Phone call with " + caller.getName() + " (Theme: " + callTheme + "):\n" + transcript);
            return "Theme: " + callTheme + "\n\nTranscript:\n" + transcript;
        }
        
        return "Call failed.";
    }

    public String getMindState() {
        return "LTM: " + mind.getLongTermMemory() + "\nSTM: " + mind.getShortTermMemory();
    }

    public void logEvent(String event) {
        mind.logEvent(event);
    }

    public String getName() {
        return entity.getName();
    }

    /**
     * Translates the state back to the Database Entity.
     * This is the "connecting class" function the user requested to translate new writes to old format.
     */
    public MatrixHuman syncToDatabaseEntity() {
        entity.setMemory(mind.getLongTermMemory());
        entity.setWorkingMemory(mind.getShortTermMemory());
        entity.setPersonality(mind.getPersonality());
        entity.setRelations(mind.getRelations());
        entity.setEventLogs(mind.getEventLogs());
        return entity;
    }
}
