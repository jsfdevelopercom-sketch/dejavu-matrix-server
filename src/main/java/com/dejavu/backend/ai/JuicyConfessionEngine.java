package com.dejavu.backend.ai;

import com.dejavu.backend.model.Confession;
import com.dejavu.backend.repository.ConfessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.concurrent.ConcurrentHashMap;

/**
 * An Agentic AI engine designed to generate deeply human, emotional,
 * simple, and mass-audience appealing "Spicy" confessions.
 * Employs a Worker-Critic loop to ensure quality and prevent duplication.
 */
@Component
public class JuicyConfessionEngine {

    @Autowired
    private GeminiAiClient geminiClient;
    
    @Autowired
    private OpenAiClient openAiClient;
    
    @Autowired
    private ConfessionRepository confessionRepository;
    
    @Autowired
    private ConfessionQualityGrader grader;

    @Autowired
    private DarkArchangelInterviewEngine interviewEngine;

    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // Global status tracker for desktop GUI feedback
    public static final ConcurrentHashMap<String, String> jobStatuses = new ConcurrentHashMap<>();

    public static class ConfessionDraft {
        public String text;
        public String locationName;
        public String placeType;
    }

    public List<Confession> generateJuicyConfessions(String themeInput, int count, String jobId) {
        if (jobId != null) jobStatuses.put(jobId, "Initializing Heavy Model for drafting...");
        List<Confession> newConfessions = new ArrayList<>();
        
        int remainingToGenerate = count;
        int enginePass = 0;
        
        /*
         * ====================================================================
         * WORKER-CRITIC GENERATION LOOP (Maximum 2 Passes)
         * ====================================================================
         * To prevent infinite loops and save API costs, we limit the engine
         * to a maximum of 2 passes. If the first pass generates 10 drafts but
         * the Critic rejects 3 of them, the second pass will attempt to
         * generate the remaining 3 drafts to hit the requested target.
         */
        while (remainingToGenerate > 0 && enginePass < 2) {
            enginePass++;
            if (jobId != null && enginePass > 1) {
                jobStatuses.put(jobId, "Pass " + enginePass + ": Shortfall detected. Generating " + remainingToGenerate + " more...");
            }
            
            // Load existing texts to avoid duplication (take 50 most recent to prevent blind spots)
            List<Confession> allExisting = confessionRepository.findAll();
            StringBuilder existingText = new StringBuilder();
            int startIdx = Math.max(0, allExisting.size() - 50);
            for (int i = startIdx; i < allExisting.size(); i++) {
                existingText.append("- ").append(allExisting.get(i).getText()).append("\n");
            }

            String workerPrompt = "You are a creative writer generating 'Juicy', 'Spicy' confessions for a mobile deduction game.\n" +
                "A 'Spicy' confession is multi-layered yet SIMPLE ON POINT, deeply involving human emotions (betrayal, regret, secret romance, petty revenge, etc.) and mass audience appeal. DO NOT use complex, poetic language.\n" +
                "The user requested this theme: '" + themeInput + "'.\n" +
                "Avoid duplicating these existing themes:\n" + existingText.toString() + "\n\n" +
                "CRITICAL LIMITS: Keep each confession under 40 words. Do NOT over-think or add explanations. Limit output time and tokens. Output MUST be short and fast.\n" +
                "Write EXACTLY " + remainingToGenerate + " new confessions. Return ONLY a valid JSON array of objects:\n" +
                "[\n" +
                "  {\n" +
                "    \"text\": \"The confession text...\",\n" +
                "    \"locationName\": \"e.g., City High School\",\n" +
                "    \"placeType\": \"e.g., school\"\n" +
                "  }\n" +
                "]";

            int attempt = 0;
            ConfessionDraft[] drafts = null;

            if (jobId != null && enginePass == 1) jobStatuses.put(jobId, "Generating " + remainingToGenerate + " drafts from heavy model...");

            // Limit to 2 attempts for JSON parsing per pass
            while (drafts == null && attempt < 2) {
                attempt++;
                String workerResponse = openAiClient.generateContent(workerPrompt); // Switch to OpenAI due to Gemini quota
                
                if (workerResponse != null) {
                    try {
                        workerResponse = workerResponse.replaceAll("^```json\\s*", "").replaceAll("```\\s*$", "").trim();
                        drafts = objectMapper.readValue(workerResponse, ConfessionDraft[].class);
                    } catch (Exception e) {
                        System.err.println("Worker JSON parse failed: " + e.getMessage());
                        workerPrompt += "\n\nFeedback: Your last output was invalid JSON. Please return STRICTLY a valid JSON array only.";
                        if (jobId != null) jobStatuses.put(jobId, "JSON parsing failed. Retrying draft generation...");
                    }
                }
            }

            if (drafts != null) {
                int index = 0;
                for (ConfessionDraft draft : drafts) {
                    index++;
                    if (jobId != null) jobStatuses.put(jobId, "Pass " + enginePass + ": Evaluating " + index + " of " + drafts.length + "... (Total Approved: " + newConfessions.size() + "/" + count + ")");
                    
                    /*
                     * ---------------------------------------------------------
                     * CRITIC PASS (Fast evaluation)
                     * ---------------------------------------------------------
                     * We use a fast, inline evaluation to immediately reject
                     * drafts that sound too robotic, childish, or lack the
                     * deep emotional resonance required by Deja-Vu's rules.
                     */
                    String criticPrompt = "Judge this confession: \"" + draft.text + "\"\n" +
                        "Criteria: Relatable, simple, juicy/emotional, mass audience appeal.\n" +
                        "Return exactly 'PASS' or 'FAIL'.";
                        
                    String criticResponse = openAiClient.generateContent(criticPrompt); // Switch to OpenAI
                    
                    if (criticResponse != null && criticResponse.contains("PASS")) {
                        Confession c = new Confession();
                        c.setText(draft.text);
                        c.setLocationName(draft.locationName);
                        c.setPlaceType(draft.placeType);
                        c.setSeeded(true);
                        
                        /*
                         * -----------------------------------------------------
                         * FINAL STORY EXTENSION & STORAGE
                         * -----------------------------------------------------
                         * If the Critic and the Quality Grader both approve the
                         * confession, it is saved to the database. We then
                         * immediately hand it over to the Dark Archangel
                         * Interview Engine, which simulates a brutal interrogation
                         * to extract the 'Extended Story' (the hidden context).
                         */
                        if (jobId != null) jobStatuses.put(jobId, "Pass " + enginePass + ": Draft " + index + " PASS. Grading using Writing School Rubric...");
                        ConfessionQualityGrader.GradingResult result = grader.gradeConfession(draft.text);
                        if (result != null) {
                            c.setSpicy(result.isSpicy);
                            if (!result.decision.contains("REJECT") && !result.decision.contains("NOT_PLAYABLE")) {
                                Confession saved = confessionRepository.save(c);
                                interviewEngine.generateGameContent(saved);
                                newConfessions.add(saved);
                            }
                        } else {
                            c.setSpicy(true);
                            Confession saved = confessionRepository.save(c);
                            interviewEngine.generateGameContent(saved);
                            newConfessions.add(saved);
                        }
                    }
                    if (newConfessions.size() >= count) break;
                }
            } else {
                 if (jobId != null) jobStatuses.put(jobId, "Failed to generate any valid drafts in pass " + enginePass);
            }
            
            remainingToGenerate = count - newConfessions.size();
        }
        
        if (jobId != null) jobStatuses.put(jobId, "DONE");
        return newConfessions;
    }
}
