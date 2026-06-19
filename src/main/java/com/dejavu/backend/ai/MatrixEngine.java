package com.dejavu.backend.ai;

import com.dejavu.backend.ai.agent.ActiveMatrixAgent;
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
    private MemoryCondenser memoryCondenser;

    @Autowired
    private AiOutputJudge outputJudge;

    @Autowired
    private PersonalityEngine personalityEngine;

    @Autowired
    private RelationsEngine relationsEngine;

    @Autowired
    private MatrixHumanRepository humanRepository;
    
    @Autowired
    private ConfessionRepository confessionRepository;
    
    @Autowired
    private com.dejavu.backend.repository.RamonNotificationRepository notificationRepository;
    
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
                    com.dejavu.backend.ai.agent.ActiveMatrixAgent agent = getAgent(human);
                    agent.logEvent("[GLOBAL EVENT DIRECT INVOLVEMENT]: " + eventDescription);
                    String reaction = agent.ponder("I was directly involved in this world event: " + eventDescription);
                    agent.logEvent("[DIRECT EVENT REACTION]: " + reaction.trim());
                    
                    // Force inject into Deep Memory
                    java.time.ZonedDateTime ncrTime = java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Kolkata"));
                    String timeStr = ncrTime.format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy"));
                    String currentMemory = human.getMemory() != null ? human.getMemory() : "";
                    agent.syncToDatabaseEntity(); // Apply first to update entity
                    human.setMemory("<span style='color:#c8c8c8'>[WORLD EVENT: " + timeStr + "] " + eventDescription + "</span><br>\n" + currentMemory);
                    
                    humanRepository.save(human);
                    result.append("Involved human updated: ").append(agent.getName()).append("\n");
                }
            }
        }

        List<MatrixHuman> allHumans = humanRepository.findAll();
        for (MatrixHuman h : allHumans) {
            if (involvedHumanIds == null || !involvedHumanIds.contains(h.getId())) {
                com.dejavu.backend.ai.agent.ActiveMatrixAgent agent = getAgent(h);
                agent.logEvent("[HEARD WORLD NEWS]: " + eventDescription);
                String reaction = agent.ponder("I heard this news: " + eventDescription);
                agent.logEvent("[WORLD NEWS REACTION]: " + reaction.trim());
                
                // Force inject into Deep Memory
                java.time.ZonedDateTime ncrTime = java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Kolkata"));
                String timeStr = ncrTime.format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy"));
                String currentMemory = h.getMemory() != null ? h.getMemory() : "";
                agent.syncToDatabaseEntity();
                h.setMemory("<span style='color:#c8c8c8'>[WORLD EVENT: " + timeStr + "] " + eventDescription + "</span><br>\n" + currentMemory);
                
                humanRepository.save(h);
            }
        }

        triggerGlobalTimeStep();
        return result.toString();
    }

    /**
     * Spawns a new MatrixHuman with a rich, dynamically generated persona using OpenAI.
     * Generates a JSON profile containing demographics, personality points, and relations.
     * The human is explicitly situated in the NCR (Delhi/Noida/Gurgaon).
     * @param params Optional tuning parameters (e.g., "Make them a sad artist").
     * @return The saved MatrixHuman entity.
     */
    public MatrixHuman spawnHuman(String name, String params) {
        String systemPrompt = "You are the Matrix Genesis Engine. Your task is to generate a complex human persona for a simulation situated in the NCR. Generate a concise but rich background. Provide an authentic Indian or American name if no name is explicitly given. Output ONLY raw JSON.";
        
        String n = (name != null && !name.trim().isEmpty()) ? name.trim() : "Randomize a culturally appropriate Indian or American name";
        String p = (params != null && !params.trim().isEmpty()) ? params.trim() : "Random";
        
        String userPrompt = "Generate a JSON with the following keys: name, age, gender, occupation, city, personality (long paragraph), relations (long paragraph).\nCRITICAL INSTRUCTION: The 'name' field MUST be: " + n + "\nUser Params: " + p;
        
        String json = openAiClient.generateContent(systemPrompt, userPrompt);
        if (json != null) {
            try {
                json = json.replaceAll("^```(?:json)?\\s*", "").replaceAll("```\\s*$", "").trim();
                
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                java.util.Map<String, Object> obj = mapper.readValue(json.trim(), java.util.Map.class);
                MatrixHuman human = new MatrixHuman();
                
                if (name != null && !name.trim().isEmpty()) {
                    human.setName(name.trim());
                } else {
                    human.setName(obj.containsKey("name") ? obj.get("name").toString() : "Unknown");
                }
                
                int age = 25;
                if (obj.containsKey("age")) {
                    try {
                        age = Integer.parseInt(obj.get("age").toString().replaceAll("[^0-9]", ""));
                    } catch (Exception ex) {}
                }
                human.setAge(age);
                human.setGender(obj.containsKey("gender") ? obj.get("gender").toString() : "Unknown");
                human.setOccupation(obj.containsKey("occupation") ? obj.get("occupation").toString() : "Unemployed");
                human.setCity(obj.containsKey("city") ? obj.get("city").toString() : "Delhi NCR");
                human.setPersonality(obj.containsKey("personality") ? obj.get("personality").toString() : "");
                human.setRelations(obj.containsKey("relations") ? obj.get("relations").toString() : "");
                human.setMemory("Day 0: Born into the Matrix.\n");
                human.setEventLogs("Day 0: Spawned into existence.\n");
                human.setWorkingMemory("Just spawned. I feel disoriented but alive. Where am I?");
                human.setCurrentDay(0);
                
                new Thread(() -> {
                    try {
                        String prompt = "A cinematic, photorealistic portrait of a " + human.getAge() + " year old " + human.getGender() + " from " + human.getCity() + ". Occupation: " + human.getOccupation() + ". " + human.getPersonality();
                        if (prompt.length() > 900) prompt = prompt.substring(0, 900);
                        prompt += " Dark, cybernetic lighting.";
                        String url = openAiClient.generateImage(prompt);
                        if (url != null) {
                            String filename = java.util.UUID.randomUUID() + ".png";
                            java.nio.file.Path path = java.nio.file.Paths.get("data/avatars/" + filename);
                            java.nio.file.Files.createDirectories(path.getParent());
                            java.io.InputStream in = new java.net.URL(url).openStream();
                            java.nio.file.Files.copy(in, path, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                            human.setAvatarUrl("/api/matrix/avatars/" + filename);
                            humanRepository.save(human);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }).start();

                return humanRepository.save(human);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        // Fallback if AI fails (e.g. rate limits)
        MatrixHuman human = new MatrixHuman();
        human.setName((name != null && !name.trim().isEmpty()) ? name.trim() : "Rajesh Kumar " + (int)(Math.random()*1000));
        human.setAge(35);
        human.setGender("Male");
        human.setOccupation("System Auditor");
        human.setCity("Delhi NCR");
        human.setPersonality("Cold, calculating, and relentlessly efficient. Observes everything.");
        human.setRelations("No known relations. Loyal only to the Matrix.");
        human.setMemory("Day 0: Born into the Matrix via Fallback Protocol.\n");
        human.setEventLogs("Day 0: Spawned into existence (Fallback).\n");
        human.setCurrentDay(0);
        return humanRepository.save(human);
    }

    /**
     * Awakens the Matrix by triggering a simulated 10-minute "day" for ALL humans in the database concurrently.
     * Uses a bounded thread pool to avoid Thread Explosion and DB connection starvation.
     */
    public void awakenMatrix() {
        new Thread(() -> {
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
        }).start();
    }

    public ActiveMatrixAgent getAgent(MatrixHuman human) {
        return new com.dejavu.backend.ai.agent.ActiveMatrixAgent(human, openAiClient, geminiAiClient, memoryCondenser, outputJudge, personalityEngine, relationsEngine);
    }

    private void simulateDay(MatrixHuman human) {
        ActiveMatrixAgent agent = getAgent(human);
        
        List<MatrixWorldMemory> worldMemories = worldMemoryRepository.findAll();
        StringBuilder newsFeed = new StringBuilder();
        if (!worldMemories.isEmpty()) {
            newsFeed.append("Current World News (Word of mouth/Social Media): ");
            int start = Math.max(0, worldMemories.size() - 5);
            for (int i = start; i < worldMemories.size(); i++) {
                newsFeed.append("- ").append(worldMemories.get(i).getEventDescription()).append(" ");
            }
        }

        String thoughts = agent.experienceDay(newsFeed.toString());
        humanRepository.save(agent.syncToDatabaseEntity());
        
        // Organic Calling Check
        if (thoughts != null) {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("<CALL:\\s*([^>]+)>").matcher(thoughts);
            if (m.find()) {
                String target = m.group(1).trim();
                MatrixHuman targetHuman = humanRepository.findFirstByNameContainingIgnoreCase(target);
                if (targetHuman != null && !targetHuman.getId().equals(human.getId())) {
                    phoneCall(human.getId(), targetHuman.getId());
                }
            }
            if (thoughts.contains("<CALL_RAMON>")) {
                com.dejavu.backend.model.RamonNotification notif = new com.dejavu.backend.model.RamonNotification();
                notif.setHumanId(human.getId());
                notif.setHumanName(human.getName());
                notif.setMessage(agent.getName() + " is desperately calling for Ramon! Mind State:\n" + agent.getMindState());
                notificationRepository.save(notif);
                
                // Add a comfort thought
                agent.ponder("I called out to Ramon. I hope he hears me.");
                humanRepository.save(agent.syncToDatabaseEntity());
            }
        }
    }

    public String phoneCall(Long callerId, Long receiverId) {
        MatrixHuman caller = humanRepository.findById(callerId).orElse(null);
        MatrixHuman receiver = humanRepository.findById(receiverId).orElse(null);
        
        if (caller == null || receiver == null) return "Humans not found.";
        
        com.dejavu.backend.ai.agent.ActiveMatrixAgent callerAgent = getAgent(caller);
        com.dejavu.backend.ai.agent.ActiveMatrixAgent receiverAgent = getAgent(receiver);
        
        String result = receiverAgent.receiveCall(callerAgent);
        
        humanRepository.save(callerAgent.syncToDatabaseEntity());
        humanRepository.save(receiverAgent.syncToDatabaseEntity());
        
        triggerGlobalTimeStep();
        return result;
    }

    public String forceConfession(Long humanId) {
        MatrixHuman human = humanRepository.findById(humanId).orElse(null);
        if (human == null) return "Human not found.";
        
        new Thread(() -> {
            String eventPrompt = "You are the Matrix Genesis Engine. Generate a single, dark, odd, or juicy confession-worthy event that just happened to this human today. It must be specific, grounded in their reality, and severe enough to confess to the Dark Archangel.";
            String userPrompt = "Name: " + human.getName() + "\nPersonality: " + human.getPersonality() + "\nMemory: " + human.getMemory();
            String event = openAiClient.generateContent(eventPrompt, userPrompt);
            
            if (event != null) {
                com.dejavu.backend.ai.agent.ActiveMatrixAgent agent = getAgent(human);
                agent.ponder("DARK EVENT: " + event);
                humanRepository.save(agent.syncToDatabaseEntity());
                
                Confession confession = new Confession();
                confession.setText(event);
                
                archangelEngine.interviewAndExpand(confession, com.dejavu.backend.controller.AdminController.getGlobalMaxQuestions());
            }
        }).start();
        
        return "Human " + human.getName() + " is being forced to confess in the background. Check logs later.";
    }

    public String injectThought(Long id, String thought) {
        MatrixHuman human = humanRepository.findById(id).orElse(null);
        if (human == null) return "Human not found.";

        com.dejavu.backend.ai.agent.ActiveMatrixAgent agent = getAgent(human);
        agent.logEvent("[SUDDEN THOUGHT INJECTED]: " + thought);
        
        String systemPrompt = "You are simulating the immediate aftermath of a human in the Matrix receiving a sudden injected thought or desire. Describe their next 10 minutes. How do they react to this desire? Do they act on it? Keep it to a single paragraph.";
        String userPrompt = "Human Details:\nName: " + agent.getName() + "\nRecent Mind State:\n" + agent.getMindState() + "\n\nInjected Thought: " + thought + "\n\nTASK: Describe their immediate reaction and actions.";
        
        String reaction = openAiClient.generateContent(systemPrompt, userPrompt);
        if (reaction != null) {
            agent.ponder("Reaction to thought: " + reaction.trim());
            humanRepository.save(agent.syncToDatabaseEntity());
            triggerGlobalTimeStep();
            return "Thought injected. Reaction: " + reaction.trim();
        }
        
        humanRepository.save(agent.syncToDatabaseEntity());
        triggerGlobalTimeStep();
        return "Thought injected, but reaction simulation failed.";
    }

    /**
     * Unfreezes the human and allows direct chat. The human thinks the user is a mystery man named Ramon.
     */
    public String chatWithHuman(Long humanId, String userMessage) {
        MatrixHuman human = humanRepository.findById(humanId).orElse(null);
        if (human == null) return "Human not found.";

        com.dejavu.backend.ai.agent.ActiveMatrixAgent agent = getAgent(human);
        String reply = agent.speak("a mysterious man named Ramon", userMessage);
        
        if (reply != null) {
            agent.ponder("[Chat with mystery man Ramon] Ramon: " + userMessage + " | My Reply: " + reply.trim());
            humanRepository.save(agent.syncToDatabaseEntity());
            triggerGlobalTimeStep();
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

        // Randomly select one human to respond, keeping them complete in TOTO
        java.util.Collections.shuffle(humans);
        MatrixHuman chosenHuman = humans.get(0);
        com.dejavu.backend.ai.agent.ActiveMatrixAgent agent = getAgent(chosenHuman);

        String context = "Recent Chat History:\n" + chatHistory + "\n";
        if (godMessage != null && !godMessage.trim().isEmpty()) {
            context += "\nGod has just announced: " + godMessage + "\n";
        }
        context += "\nRead the chat history and provide your next message. You MUST respond naturally based on your deeply held memories and your current psychological state. If you recently experienced trauma, violation, or joy, it MUST bleed into your tone. Output ONLY your message text, without prefixing your name.";

        String replyText = agent.speak("the entire Town Square Group Chat", context);
        
        if (replyText != null && !replyText.trim().isEmpty()) {
            String fullReply = agent.getName() + ": " + replyText.trim();
            agent.ponder("I spoke in the town square: " + replyText.trim());
            humanRepository.save(agent.syncToDatabaseEntity());
            
            triggerGlobalTimeStep();
            return fullReply;
        }
        
        triggerGlobalTimeStep();
        return "System: Silence in the town square.";
    }

    private final java.util.concurrent.atomic.AtomicBoolean isGlobalSimRunning = new java.util.concurrent.atomic.AtomicBoolean(false);

    /**
     * Ensures universal time flow. If one is unfrozen, all are unfrozen.
     * Prevents overlapping massive API calls using an AtomicBoolean.
     */
    public void triggerGlobalTimeStep() {
        if (isGlobalSimRunning.compareAndSet(false, true)) {
            new Thread(() -> {
                try {
                    awakenMatrix();
                } finally {
                    isGlobalSimRunning.set(false);
                }
            }).start();
        }
    }

    public String processAllIncompleteConfessions() {
        new Thread(() -> {
            List<Confession> confessions = confessionRepository.findAll();
            for (Confession c : confessions) {
                if (c.getExtendedStory() == null || c.getExtendedStory().trim().isEmpty()) {
                    try {
                        archangelEngine.interviewAndExpand(c, com.dejavu.backend.controller.AdminController.getGlobalMaxQuestions());
                        Thread.sleep(3000); // Wait 3s to avoid rate limits
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            
            List<MatrixHuman> smiths = humanRepository.findAll();
            for (MatrixHuman h : smiths) {
                if (h.getName() != null && h.getName().toLowerCase().contains("agent smith")) {
                    try {
                        String sysP = "You are the Matrix Genesis Engine. Generate a complex human persona in the NCR. Provide an authentic Indian or American name. Output ONLY raw JSON.";
                        String usrP = "Generate a JSON with keys: name, age, gender, occupation, city, personality (long paragraph), relations (long paragraph).";
                        String json = openAiClient.generateContent(sysP, usrP);
                        if (json != null) {
                            json = json.replaceAll("^```(?:json)?\\s*", "").replaceAll("```\\s*$", "").trim();
                            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                            java.util.Map<String, Object> obj = mapper.readValue(json.trim(), java.util.Map.class);
                            
                            h.setName(obj.containsKey("name") ? obj.get("name").toString() : "Unknown");
                            int age = 25;
                            try { age = Integer.parseInt(obj.get("age").toString().replaceAll("[^0-9]", "")); } catch (Exception ex) {}
                            h.setAge(age);
                            h.setGender(obj.containsKey("gender") ? obj.get("gender").toString() : "Unknown");
                            h.setOccupation(obj.containsKey("occupation") ? obj.get("occupation").toString() : "Unemployed");
                            h.setCity(obj.containsKey("city") ? obj.get("city").toString() : "Delhi NCR");
                            h.setPersonality(obj.containsKey("personality") ? obj.get("personality").toString() : "");
                            h.setRelations(obj.containsKey("relations") ? obj.get("relations").toString() : "");
                            
                            h.setAvatarUrl(null); // Force avatar regen
                            humanRepository.save(h);
                            Thread.sleep(3000);
                        }
                    } catch(Exception e) {}
                }
            }
            
            List<MatrixHuman> humans = humanRepository.findAll();
            for (MatrixHuman h : humans) {
                if (h.getAvatarUrl() == null || h.getAvatarUrl().trim().isEmpty() || h.getAvatarUrl().equals("null")) {
                    try {
                        String appearanceDesc = "A " + h.getAge() + " year old " + h.getGender() + " from " + h.getCity() + " working as a " + h.getOccupation() + ". Dressed in highly fashionable, modern western clothing (e.g. sharp suits, designer jackets, casual chic western wear).";
                        String prompt = "Full body, head-to-toe wide-angle shot. Cinematic, photorealistic. " + appearanceDesc + " " + h.getPersonality();
                        if (prompt.length() > 900) prompt = prompt.substring(0, 900);
                        prompt += " Dark, cybernetic lighting. Standing upright, whole body visible, showing feet and shoes.";
                        String url = openAiClient.generateImage(prompt);
                        
                        // Inject appearance into their memory so they know what they look like
                        com.dejavu.backend.ai.agent.ActiveMatrixAgent agent = getAgent(h);
                        agent.ponder("[SELF REFLECTION]: I caught a glimpse of myself in the mirror. " + appearanceDesc);
                        h = agent.syncToDatabaseEntity();

                        if (url != null) {
                            String filename = java.util.UUID.randomUUID() + ".png";
                            java.nio.file.Path path = java.nio.file.Paths.get("data/avatars/" + filename);
                            java.nio.file.Files.createDirectories(path.getParent());
                            java.io.InputStream in = new java.net.URL(url).openStream();
                            java.nio.file.Files.copy(in, path, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                            h.setAvatarUrl("/api/matrix/avatars/" + filename);
                            humanRepository.save(h);
                            Thread.sleep(3000);
                        }
                    } catch (Exception e) {}
                }
            }
        }).start();
        return "Started background processing of all incomplete confessions AND missing avatars. Check server logs.";
    }
}
