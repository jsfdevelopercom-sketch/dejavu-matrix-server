package com.dejavu.backend.ai;

import com.dejavu.backend.model.Confession;
import com.dejavu.backend.model.MatrixHuman;
import com.dejavu.backend.model.MatrixWorldMemory;
import com.dejavu.backend.repository.ConfessionRepository;
import com.dejavu.backend.repository.MatrixHumanRepository;
import com.dejavu.backend.repository.MatrixWorldMemoryRepository;
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
    private MatrixMindEngine matrixMindEngine;
    
    @Autowired
    private DarkArchangelInterviewEngine archangelEngine;
    
    
    
    @Autowired
    private MatrixWorldMemoryRepository worldMemoryRepository;

    /**
     * Injects a global binding event into the Matrix World Memory.
     * Optionally ties it to specific humans who were directly involved.
     */
    public String injectWorldEvent(String eventDescription, List<Long> involvedHumanIds) {
        MatrixWorldMemory worldEvent = new MatrixWorldMemory();
        worldEvent.setEventDescription(eventDescription);
        worldMemoryRepository.save(worldEvent);

        StringBuilder result = new StringBuilder("World Event injected successfully.\n");

        if (involvedHumanIds != null && !involvedHumanIds.isEmpty()) {
            for (Long id : involvedHumanIds) {
                MatrixHuman human = humanRepository.findById(id).orElse(null);
                if (human != null) {
                    human.setMemory(human.getMemory() + "\n[GLOBAL EVENT DIRECT INVOLVEMENT]: " + eventDescription);
                    humanRepository.save(human);
                    result.append("Involved human updated: ").append(human.getName()).append("\n");
                }
            }
        }
        return result.toString();
    }

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
        
        String json = openAiClient.generateContent(systemPrompt, userPrompt);
        if (json != null) {
            try {
                json = json.replaceAll("^```(?:json)?\\s*", "").replaceAll("```\\s*$", "").trim();
                
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
        
        // Fallback if AI fails (e.g. rate limits)
        MatrixHuman human = new MatrixHuman();
        human.setName("Agent Smith " + (int)(Math.random()*1000));
        human.setAge(35);
        human.setGender("Male");
        human.setOccupation("System Auditor");
        human.setCity("Delhi NCR");
        human.setPersonality("Cold, calculating, and relentlessly efficient. Observes everything.");
        human.setRelations("No known relations. Loyal only to the Matrix.");
        human.setMemory("Day 0: Born into the Matrix via Fallback Protocol.\n");
        human.setCurrentDay(0);
        return humanRepository.save(human);
    }

    /**
     * Awakens the Matrix by triggering a simulated 10-minute "day" for ALL humans in the database concurrently.
     * Uses a bounded thread pool to avoid Thread Explosion and DB connection starvation.
     */
    public void awakenMatrix() {
        List<MatrixHuman> humans = humanRepository.findAll();
        ExecutorService executor = Executors.newFixedThreadPool(10);
        for (MatrixHuman human : humans) {
            try {
                Thread.sleep(6000); // 6s delay to prevent Free Tier 15 RPM Rate Limit
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
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

    public void updateWorkingMemory(MatrixHuman human, String newEvent) {
        String wm = human.getWorkingMemory();
        if (wm == null) wm = "";
        java.time.ZonedDateTime ncrTime = java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Kolkata"));
        String timeStr = ncrTime.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a 'NCR Time'"));
        String stampedEvent = "[" + timeStr + "] " + newEvent;
        wm = stampedEvent + "\n\n" + wm;
        
        String logs = human.getEventLogs();
        if (logs == null) logs = "";
        logs = stampedEvent + "\n\n" + logs;
        human.setEventLogs(logs);
        
        // Use OpenAI to evaluate if any memory should be shifted to LTM (Single Liner)
        String prompt = "Review this Working Memory of " + human.getName() + ".\n" + wm + "\n\nIs there a highly significant event (very good, bad, scary, novel, important)? If YES, output exactly ONE sentence summarizing it to permanent memory. If NO, output 'NONE'.";
        String ltm = openAiClient.generateContent(prompt);
        
        if (ltm != null && !ltm.contains("NONE") && ltm.length() > 5) {
            String memory = human.getMemory();
            if (memory == null) memory = "";
            memory = "[CORE MEMORY]: " + ltm.trim() + "\n" + memory;
            human.setMemory(memory);
            wm = ""; // Prune working memory after LTM extraction
        }
        
        if (wm.length() > 5000) wm = wm.substring(0, 5000);
        human.setWorkingMemory(wm);
        humanRepository.save(human);
    }

    private void simulateDay(MatrixHuman human) {
        human.setCurrentDay(human.getCurrentDay() + 1);
        
        List<MatrixWorldMemory> worldMemories = worldMemoryRepository.findAll();
        StringBuilder newsFeed = new StringBuilder();
        if (!worldMemories.isEmpty()) {
            newsFeed.append("Current World News (Word of mouth/Social Media): ");
            int start = Math.max(0, worldMemories.size() - 5);
            for (int i = start; i < worldMemories.size(); i++) {
                newsFeed.append("- ").append(worldMemories.get(i).getEventDescription()).append(" ");
            }
        }

        String systemPrompt = "You are simulating a day in the life of a human in the Matrix. Run their routine for 10 simulated minutes which equals an entire day. Produce a concise narrative of the events of their day. " + newsFeed.toString();
        String userPrompt = "Human Details:\nName: " + human.getName() + "\nAge: " + human.getAge() + "\nOccupation: " + human.getOccupation() + "\nPersonality: " + human.getPersonality() + "\nLTM: " + human.getMemory() + "\nSTM: " + human.getWorkingMemory() + "\n\nTASK: Describe the events of their Day " + human.getCurrentDay() + ".";
        
        String rawDayEvents = openAiClient.generateContent(systemPrompt, userPrompt);
        if (rawDayEvents != null) {
            String consciousThoughts = matrixMindEngine.processEventToMind(human, rawDayEvents);
            updateWorkingMemory(human, "Day " + human.getCurrentDay() + " [Internal Mind State]:\n" + consciousThoughts.trim());
            
            // Proactive Calling Check
            String callDesirePrompt = "Based on your day and your memories:\n" + human.getMemory() + "\n" + human.getWorkingMemory() + "\nDo you desperately want to call someone specific? Output their exact name or NONE.";
            String target = openAiClient.generateContent(callDesirePrompt);
            if (target != null && !target.contains("NONE")) {
                MatrixHuman targetHuman = humanRepository.findFirstByNameContainingIgnoreCase(target.trim());
                if (targetHuman != null && !targetHuman.getId().equals(human.getId())) {
                    phoneCall(human.getId(), targetHuman.getId());
                }
            }
        }
    }

    public String phoneCall(Long callerId, Long receiverId) {
        MatrixHuman caller = humanRepository.findById(callerId).orElse(null);
        MatrixHuman receiver = humanRepository.findById(receiverId).orElse(null);
        
        if (caller == null || receiver == null) return "Humans not found.";
        
        // 1. Acceptance Engine
        String acceptPrompt = "You are " + receiver.getName() + ".\n" +
            "Your personality: " + receiver.getPersonality() + "\nYour LTM: " + receiver.getMemory() + "\nYour STM: " + receiver.getWorkingMemory() + "\n" +
            "Incoming call from " + caller.getName() + ". Based on your past experiences with them, do you ACCEPT or REJECT? Reply with exactly one word: ACCEPT or REJECT.";
        
        String decision = openAiClient.generateContent(acceptPrompt);
        if (decision != null && decision.contains("REJECT")) {
            updateWorkingMemory(caller, "[CALL REJECTED/CUT] Tried calling " + receiver.getName() + " but they rejected/cut the call.");
            updateWorkingMemory(receiver, "[CALL REJECTED/CUT] Rejected an incoming call from " + caller.getName() + ".");
            return receiver.getName() + " rejected the call.";
        }
        
        // 2. Intention & Transcript Engine
        String intentPrompt = "Analyze these humans and define the secret intention of a phone call today. 1 sentence.\n" +
                "Caller: " + caller.getName() + " | LTM: " + caller.getMemory() + " | STM: " + caller.getWorkingMemory() + "\n" +
                "Receiver: " + receiver.getName() + " | LTM: " + receiver.getMemory() + " | STM: " + receiver.getWorkingMemory();
        
        String callTheme = openAiClient.generateContent(intentPrompt);
        if (callTheme == null) callTheme = "Casual catch-up.";

        String transcriptPrompt = "Write a realistic transcript between " + caller.getName() + " and " + receiver.getName() + " about: " + callTheme + "\n" +
                "CRITICAL: Use their full memories (LTM and STM) to inform the dialogue. Do not ignore past conflicts or major events!\n" +
                "Caller LTM: " + caller.getMemory() + "\nCaller STM: " + caller.getWorkingMemory() + "\n" +
                "Receiver LTM: " + receiver.getMemory() + "\nReceiver STM: " + receiver.getWorkingMemory();
                
        String transcript = openAiClient.generateContent(transcriptPrompt);
        
        if (transcript != null) {
            updateWorkingMemory(caller, "Phone call with " + receiver.getName() + " (Theme: " + callTheme + "):\n" + transcript);
            updateWorkingMemory(receiver, "Phone call with " + caller.getName() + " (Theme: " + callTheme + "):\n" + transcript);
            return "Theme: " + callTheme + "\n\nTranscript:\n" + transcript;
        }
        return "Call failed.";
    }

    public String forceConfession(Long humanId) {
        MatrixHuman human = humanRepository.findById(humanId).orElse(null);
        if (human == null) return "Human not found.";
        
        String eventPrompt = "You are the Matrix Genesis Engine. Generate a single, dark, odd, or juicy confession-worthy event that just happened to this human today. It must be specific, grounded in their reality, and severe enough to confess to the Dark Archangel.";
        String userPrompt = "Name: " + human.getName() + "\nPersonality: " + human.getPersonality() + "\nMemory: " + human.getMemory();
        String event = openAiClient.generateContent(eventPrompt, userPrompt);
        
        if (event == null) return "Failed to generate event.";
        
        updateWorkingMemory(human, "DARK EVENT: " + event);
        
        Confession confession = new Confession();
        confession.setText(event);
        
        Confession saved = archangelEngine.interviewAndExpand(confession, com.dejavu.backend.controller.AdminController.getGlobalMaxQuestions());
        return "Human " + human.getName() + " confessed: " + saved.getText() + "\n\nArchangel's Judgment: " + saved.getExtendedStory();
    }

    public String injectThought(Long id, String thought) {
        MatrixHuman human = humanRepository.findById(id).orElse(null);
        if (human == null) return "Human not found.";

        updateWorkingMemory(human, "[SUDDEN THOUGHT INJECTED]: " + thought);
        
        String systemPrompt = "You are simulating the immediate aftermath of a human in the Matrix receiving a sudden injected thought or desire. Describe their next 10 minutes. How do they react to this desire? Do they act on it? Keep it to a single paragraph.";
        String userPrompt = "Human Details:\nName: " + human.getName() + "\nPersonality: " + human.getPersonality() + "\nRecent STM:\n" + human.getWorkingMemory() + "\n\nInjected Thought: " + thought + "\n\nTASK: Describe their immediate reaction and actions.";
        
        String reaction = openAiClient.generateContent(systemPrompt, userPrompt);
        if (reaction != null) {
            updateWorkingMemory(human, "Reaction to thought: " + reaction.trim());
            return "Thought injected. Reaction: " + reaction.trim();
        }
        
        return "Thought injected, but reaction simulation failed.";
    }

    /**
     * Unfreezes the human and allows direct chat. The human thinks the user is a mystery man named Ramon.
     */
    public String chatWithHuman(Long humanId, String userMessage) {
        MatrixHuman human = humanRepository.findById(humanId).orElse(null);
        if (human == null) return "Human not found.";

        String systemPrompt = "You are roleplaying as a human inside the Matrix. You have suddenly been contacted by a mysterious man named 'Ramon'. React naturally according to your personality, current situation, and recent memories. Reply in the first person.\n" +
                "Name: " + human.getName() + "\n" +
                "Age: " + human.getAge() + "\n" +
                "Occupation: " + human.getOccupation() + "\n" +
                "Personality: " + human.getPersonality() + "\n" +
                "Recent Memory: " + human.getMemory();

        String prompt = "Ramon says: \"" + userMessage + "\"\n" +
                "Respond to Ramon:";

        String reply = geminiAiClient.generateContentLight(systemPrompt + "\n\n" + prompt);
        if (reply != null) {
            String updatedMemory = human.getMemory() + "\n[Chat with mystery man Ramon] Ramon: " + userMessage + " | My Reply: " + reply.trim();
            if (updatedMemory.length() > 60000) {
                updatedMemory = updatedMemory.substring(updatedMemory.length() - 60000);
            }
            human.setMemory(updatedMemory);
            humanRepository.save(human);
            return reply.trim();
        }
        return "Human is non-responsive.";
    }

    /**
     * Simulates a turn in the Town Square group chat.
     */
    public String townSquareTurn(String chatHistory, String godMessage) {
        List<MatrixHuman> humans = humanRepository.findAll();
        if (humans.isEmpty()) return "No humans available.";

        StringBuilder humanContext = new StringBuilder("Town Square Participants:\n");
        // Limit to 10 random humans to avoid context limit overflow
        java.util.Collections.shuffle(humans);
        int maxHumans = Math.min(humans.size(), 10);
        for (int i = 0; i < maxHumans; i++) {
            MatrixHuman h = humans.get(i);
            humanContext.append("- Name: ").append(h.getName())
                    .append(" (").append(h.getOccupation()).append(") | Personality: ")
                    .append(h.getPersonality()).append("\n");
        }

        String systemPrompt = "You are the Matrix Town Square Simulator. You must simulate ONE SINGLE turn of conversation in a group chat containing the humans listed below.\n" +
                humanContext.toString() + "\n" +
                "RULES:\n" +
                "1. Read the recent chat history.\n" +
                "2. Choose EXACTLY ONE human to respond naturally based on their personality.\n" +
                "3. If God has just spoken, someone must react to God in awe, fear, or confusion.\n" +
                "4. Output format MUST be strictly: [Human Name]: [Message]";

        String prompt = "Recent Chat History:\n" + chatHistory + "\n";
        if (godMessage != null && !godMessage.trim().isEmpty()) {
            prompt += "\nGod: " + godMessage + "\n";
        }
        prompt += "\nGenerate the next single response from one human:";

        String reply = geminiAiClient.generateContentHeavy(systemPrompt + "\n\n" + prompt);
        if (reply != null) {
            return reply.trim();
        }
        return "System: Silence in the town square.";
    }
}
