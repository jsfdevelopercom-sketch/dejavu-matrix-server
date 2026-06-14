package com.dejavu.backend.ai;

import com.dejavu.backend.model.Confession;
import com.dejavu.backend.model.MatrixHuman;
import com.dejavu.backend.repository.ConfessionRepository;
import com.dejavu.backend.repository.MatrixHumanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * MatrixEngine is the core simulation orchestrator for Project "Matrix".
 * It is responsible for simulating human agents (spawning, daily life, interaction, and forcing confessions).
 * This service runs independently of the main game loop, acting as a background sandbox to generate realistic data.
 */
@Service
public class MatrixEngine {

    @Autowired
    private OpenAiClient openAiClient;

    @Autowired
    private GeminiAiClient geminiAiClient;

    @Autowired
    private MatrixHumanRepository humanRepository;
    
    @Autowired
    private ConfessionRepository confessionRepository;
    
    @Autowired
    private DarkArchangelInterviewEngine archangelEngine;
    
    @Autowired
    private com.dejavu.backend.controller.AdminController adminController;

    /**
     * Spawns a new MatrixHuman with a rich, dynamically generated persona using OpenAI.
     * Generates a JSON profile containing demographics, personality points, and relations.
     * The human is explicitly situated in the NCR (Delhi/Noida/Gurgaon).
     * @param params Optional tuning parameters (e.g., "Make them a sad artist").
     * @return The saved MatrixHuman entity.
     */
    public MatrixHuman spawnHuman(String params) {
        String systemPrompt = "You are the Matrix Genesis Engine. Your task is to generate a deeply complex human persona for a simulation situated in the NCR (National Capital Region of India). Use real places like Noida, Gurgaon, Delhi, specific colleges, and hangout spots. Generate a minimum of 50 personality points and essential relation points (parents, siblings, issues, medical history, dreams).";
        String userPrompt = "Generate a JSON with the following keys: name, age, gender, occupation, city, personality (long paragraph), relations (long paragraph). User Params: " + (params != null ? params : "Random");
        
        String json = geminiAiClient.generateContentHeavy(systemPrompt + "\n" + userPrompt);
        if (json != null) {
            try {
                // Strip markdown backticks
                if (json.startsWith("```json")) {
                    json = json.substring(7);
                    if (json.endsWith("```")) {
                        json = json.substring(0, json.length() - 3);
                    }
                }
                
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                java.util.Map<String, Object> obj = mapper.readValue(json.trim(), java.util.Map.class);
                MatrixHuman human = new MatrixHuman();
                human.setName(obj.containsKey("name") ? obj.get("name").toString() : "Unknown");
                human.setAge(obj.containsKey("age") ? Integer.parseInt(obj.get("age").toString()) : 25);
                human.setGender(obj.containsKey("gender") ? obj.get("gender").toString() : "Unknown");
                human.setOccupation(obj.containsKey("occupation") ? obj.get("occupation").toString() : "Unemployed");
                human.setCity(obj.containsKey("city") ? obj.get("city").toString() : "Delhi NCR");
                human.setPersonality(obj.containsKey("personality") ? obj.get("personality").toString() : "");
                human.setRelations(obj.containsKey("relations") ? obj.get("relations").toString() : "");
                human.setMemory("Day 0: Born into the Matrix.\n");
                human.setCurrentDay(0);
                return humanRepository.save(human);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * Awakens the Matrix by triggering a simulated 10-minute "day" for ALL humans in the database concurrently.
     * Uses a bounded thread pool to avoid Thread Explosion and DB connection starvation.
     */
    public void awakenMatrix() {
        List<MatrixHuman> humans = humanRepository.findAll();
        ExecutorService executor = Executors.newFixedThreadPool(10);
        for (MatrixHuman human : humans) {
            executor.submit(() -> {
                try {
                    simulateDay(human);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
        executor.shutdown();
        try {
            executor.awaitTermination(30, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Internal method to simulate exactly one day for a single human.
     * Uses Gemini Heavy to parse their personality and past memory, and generates a new daily narrative.
     * Appends the output to the human's rolling memory buffer (capped at 60,000 chars to save DB space).
     * @param human The MatrixHuman to simulate.
     */
    private void simulateDay(MatrixHuman human) {
        human.setCurrentDay(human.getCurrentDay() + 1);
        String systemPrompt = "You are simulating a day in the life of a human in the Matrix. Run their routine for 10 simulated minutes which equals an entire day. Produce a concise narrative of their day, focusing on what they did, who they interacted with, and their inner thoughts. Keep it to one paragraph.";
        String userPrompt = "Human Details:\nName: " + human.getName() + "\nAge: " + human.getAge() + "\nCity: " + human.getCity() + "\nOccupation: " + human.getOccupation() + "\nPersonality: " + human.getPersonality() + "\nRelations: " + human.getRelations() + "\nPast Memory:\n" + human.getMemory() + "\n\nTASK: Describe their Day " + human.getCurrentDay() + ".";
        
        // Use Light Model to prevent token hemorrhaging at scale
        String daySimulation = geminiAiClient.generateContentLight(systemPrompt + "\n\n" + userPrompt);
        if (daySimulation != null) {
            String updatedMemory = human.getMemory() + "\nDay " + human.getCurrentDay() + ": " + daySimulation.trim();
            // Limit memory length
            if (updatedMemory.length() > 60000) {
                updatedMemory = updatedMemory.substring(updatedMemory.length() - 60000);
            }
            human.setMemory(updatedMemory);
            humanRepository.save(human);
        }
    }

    /**
     * Forces two MatrixHumans to communicate via a simulated phone call.
     * Generates a realistic transcript based on both of their recent memories and traits.
     * Injects the resulting transcript into the permanent memory of both agents.
     * @param callerId DB ID of the caller.
     * @param receiverId DB ID of the receiver.
     * @return The resulting transcript text.
     */
    public String phoneCall(Long callerId, Long receiverId) {
        MatrixHuman caller = humanRepository.findById(callerId).orElse(null);
        MatrixHuman receiver = humanRepository.findById(receiverId).orElse(null);
        
        if (caller == null || receiver == null) return "Humans not found.";
        
        String prompt = "Simulate a phone call between two humans in the NCR region. They studied at the same primary school so they are friends.\n" +
                "Caller: " + caller.getName() + " (Age " + caller.getAge() + ", " + caller.getOccupation() + ")\n" +
                "Receiver: " + receiver.getName() + " (Age " + receiver.getAge() + ", " + receiver.getOccupation() + ")\n" +
                "Caller's recent memory: " + caller.getMemory() + "\n" +
                "Receiver's recent memory: " + receiver.getMemory() + "\n\n" +
                "Write a realistic phone conversation transcript between them. Keep it under 200 words.";
                
        String transcript = geminiAiClient.generateContentHeavy(prompt);
        
        if (transcript != null) {
            caller.setMemory(caller.getMemory() + "\nCalled " + receiver.getName() + ":\n" + transcript);
            receiver.setMemory(receiver.getMemory() + "\nReceived call from " + caller.getName() + ":\n" + transcript);
            humanRepository.save(caller);
            humanRepository.save(receiver);
            return transcript;
        }
        return "Call failed.";
    }

    /**
     * Forces a human to experience a "Dark Event" and immediately confess it to the Dark Archangel.
     * This bridges the Matrix sandbox with the main Deja-Vu engine by generating high-quality
     * confession data from simulated agents.
     * @param humanId The ID of the human to interrogate.
     * @return The final judgment/extended story generated by the Archangel.
     */
    public String forceConfession(Long humanId) {
        MatrixHuman human = humanRepository.findById(humanId).orElse(null);
        if (human == null) return "Human not found.";
        
        // 1. Generate a dark event
        String eventPrompt = "You are the Matrix Genesis Engine. Generate a single, dark, odd, or juicy confession-worthy event that just happened to this human today. It must be specific, grounded in their reality, and severe enough to confess to the Dark Archangel.";
        String userPrompt = "Name: " + human.getName() + "\nPersonality: " + human.getPersonality() + "\nMemory: " + human.getMemory();
        String event = geminiAiClient.generateContentHeavy(eventPrompt + "\n" + userPrompt);
        
        if (event == null) return "Failed to generate event.";
        
        human.setMemory(human.getMemory() + "\nDARK EVENT: " + event);
        humanRepository.save(human);
        
        // 2. Human confesses to Archangel
        Confession confession = new Confession();
        confession.setText(event); // The initial confession is the event
        
        // Use Archangel to expand it
        Confession saved = archangelEngine.interviewAndExpand(confession, com.dejavu.backend.controller.AdminController.getGlobalMaxQuestions());
        return "Human " + human.getName() + " confessed: " + saved.getText() + "\n\nArchangel's Judgment: " + saved.getExtendedStory();
    }

    public String injectThought(Long id, String thought) {
        MatrixHuman human = humanRepository.findById(id).orElse(null);
        if (human == null) return "Human not found.";

        String updatedMemory = human.getMemory() + "\n[SUDDEN THOUGHT INJECTED]: " + thought;
        if (updatedMemory.length() > 60000) {
            updatedMemory = updatedMemory.substring(updatedMemory.length() - 60000);
        }
        human.setMemory(updatedMemory);
        humanRepository.save(human);
        
        // Brief sim run specifically focused on the thought
        String systemPrompt = "You are simulating the immediate aftermath of a human in the Matrix receiving a sudden injected thought or desire. Describe their next 10 minutes. How do they react to this desire? Do they act on it? Keep it to a single paragraph.";
        String userPrompt = "Human Details:\nName: " + human.getName() + "\nPersonality: " + human.getPersonality() + "\nRecent Memory:\n" + human.getMemory() + "\n\nInjected Thought: " + thought + "\n\nTASK: Describe their immediate reaction and actions.";
        
        String reaction = geminiAiClient.generateContentHeavy(systemPrompt + "\n\n" + userPrompt);
        if (reaction != null) {
            String newMemory = human.getMemory() + "\nReaction to thought: " + reaction.trim();
            if (newMemory.length() > 60000) newMemory = newMemory.substring(newMemory.length() - 60000);
            human.setMemory(newMemory);
            humanRepository.save(human);
            return "Thought injected. Reaction: " + reaction.trim();
        }
        
        return "Thought injected, but reaction simulation failed.";
    }
}
