package com.dejavu.backend.ai;

import com.dejavu.backend.model.Confession;
import com.dejavu.backend.repository.ConfessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * DarkArchangelInterviewEngine is the primary conversational processor for confessions.
 * It features two distinct pathways:
 * 1. An automated multi-agent simulation for offline/background confession expansion (`interviewAndExpand`).
 * 2. An interactive, live API endpoint for real-time mobile app users (`interactiveChat`).
 * 
 * Rules and Focals are dynamically fetched from the database, allowing live Admin tuning.
 */
@Component
public class DarkArchangelInterviewEngine {

    @Autowired
    private GeminiAiClient aiClient;

    @Autowired
    private ConfessionRepository confessionRepository;

    /**
     * Converts a basic confession into a rich, dense short story by running an autonomous
     * multi-agent simulation between the Dark Archangel and an AI-generated Mortal Persona.
     * 
     * Pipeline:
     * 1. Creates a specific mortal persona from the confession text.
     * 2. Runs an adversarial back-and-forth for `maxQuestions` rounds.
     * 3. Synthesizes the raw transcript into a highly factual, "no-fluff" extended story,
     *    which is critical for the downstream physical clue generator.
     * 
     * @param confession The initial unexpanded Confession.
     * @param maxQuestions The max depth of the interrogation.
     * @return The updated Confession with the generated extended story.
     */
    public Confession interviewAndExpand(Confession confession, int maxQuestions) {
        try {
            // 1. Initialize Mortal Persona
            String personaSystem = "You are a psychological profiler.";
            String personaUser = "Read this confession: \"" + confession.getText() + "\"\n" +
                    "Generate a realistic persona for this mortal. Include age, gender, occupation, and psychological state. Keep it under 50 words.";
            String persona = aiClient.generateContentLight(personaSystem + "\n" + personaUser);
            if (persona == null) persona = "An anonymous, guilt-ridden mortal.";

            StringBuilder transcript = new StringBuilder();
            transcript.append("Original Confession: \"").append(confession.getText()).append("\"\n\n");

            // 2. Run the Interrogation Loop
            for (int i = 1; i <= maxQuestions; i++) {
                // Archangel's Turn
                String archangelSystem = "You are the Dark Archangel, interrogating a mortal. Be brutally analytical. Keep it to 1-2 short sentences.";
                String archangelUser = "Their Confession: \"" + confession.getText() + "\"\n" +
                        "Transcript so far:\n" + transcript.toString() + "\n" +
                        "TASK: Ask exactly ONE piercing question. " +
                        (i == 1 ? "Since this is your first question, you MUST compulsorily ask for their demographics (age, gender, country, city) while probing their sin." : "");
                
                String archangelQ = aiClient.generateContentLight(archangelSystem + "\n" + archangelUser);
                if (archangelQ == null) break;
                transcript.append("ARCHANGEL: ").append(archangelQ.trim()).append("\n");

                // Mortal's Turn
                String mortalSystem = "You are roleplaying as a mortal being interrogated by a terrifying Archangel.\n" +
                        "Your Persona: " + persona + "\n" +
                        "CRITICAL RULE: When answering, you MUST invent hyper-specific, highly concrete facts (street names, exact times, specific objects, exact dollar amounts) to make your sin realistic. DO NOT use vague emotional fluff. Give hard, concrete details.\n" +
                        "Do NOT break character. Keep it under 40 words.";
                String mortalUser = "Transcript so far:\n" + transcript.toString() + "\n" +
                        "TASK: Answer the Archangel's latest question in character. Reveal your demographics if asked, or refuse. " +
                        "Show emotion (guilt, defiance, fear).";
                
                String mortalA = aiClient.generateContentLight(mortalSystem + "\n" + mortalUser);
                if (mortalA == null) break;
                transcript.append("MORTAL: ").append(mortalA.trim()).append("\n\n");
            }

            // 3. Synthesize Final Story
            String synthesisSystem = "You are an omniscient observer recording a confession. Synthesize the transcript into a dark, clinical, and precise account of the sin. DO NOT USE ADJECTIVES, METAPHORS, OR POETIC WORDS. Focus purely on the 'What', 'When', 'Why', and 'Who'. Use simple, direct language. CRITICAL RULE: NEVER mention the confessor's name, their relatives' names, or any exact geographical names/places (e.g. use 'a city park' instead of 'Clayton Park', use 'a male adolescent' instead of 'Aarav'). Replace ALL specific identities with anonymous descriptors.";
            String synthesisUser = "Review this completed interrogation transcript:\n" +
                    transcript.toString() + "\n" +
                    "TASK: Write a concrete, factual story summary based on the details revealed. \n" +
                    "End with a newline: 'Persona: [Age Group], [Gender], [Occupation/Status]'. CRITICAL: OMIT ALL specific names, exact ages, and exact places/locations.";
            
            String finalStory = aiClient.generateContentLight(synthesisSystem + "\n" + synthesisUser);
            if (finalStory != null && !finalStory.trim().isEmpty()) {
                confession.setExtendedStory(finalStory.trim());
                return confessionRepository.save(confession);
            }
        } catch (Exception e) {
            System.err.println("Multi-Agent Interview failed: " + e.getMessage());
        }
        
        // Fallback
        confession.setExtendedStory(confession.getText());
        return confessionRepository.save(confession);
    }

    public static class ChatRequest {
        public String language;
        public String locationName;
        public String placeType;
        public java.util.List<Message> history;
        public static class Message {
            public String role; // "user" or "angel"
            public String text;
        }
    }

    public static class ChatResponse {
        public String reply;
        public boolean isFinal;
    }

    /**
     * Generates a fast, dark, funny waiting text while the heavier AI model processes the real response.
     * Used exclusively to improve UX latency on the frontend by immediately returning a filler string.
     * 
     * @param request The current state of the chat history.
     * @return A snappy, condescending "thinking" phrase.
     */
    public String generateFiller(ChatRequest request) {
        String lastUserText = request.history.get(request.history.size() - 1).text;
        String systemPrompt = "You are the Dark Archangel. The mortal just said: '" + lastUserText + "'. " +
                "You are currently thinking/weighing their soul. " +
                "Generate exactly ONE short, dark, yet funny 'filler' sentence to show you are thinking. " +
                "Do NOT use poetic words like 'shadows'. Be mocking and impatient. Example: 'Let me consult the ledger of your pathetic choices...'";
        
        String filler = aiClient.generateContentLight(systemPrompt + "\nTASK: Generate thinking filler.");
        return filler != null ? filler.trim() : "The Archangel is weighing your pathetic words...";
    }

    @Autowired
    private com.dejavu.backend.repository.PromptConfigRepository promptConfigRepository;

    /**
     * Fetches dynamic AI prompt configurations from the database (e.g., rules, focals).
     * If the configuration isn't found, it defaults to the provided `defaultValue`.
     * This enables live tweaking of the Archangel's personality without a server restart.
     */
    private String getPromptConfig(String key, String defaultValue) {
        return promptConfigRepository.findById(key)
            .map(com.dejavu.backend.model.PromptConfig::getPromptContent)
            .orElse(defaultValue);
    }

    /**
     * Handles a single turn of the interactive user confession interview from the mobile app.
     * This endpoint parses the entire chat history and either:
     * A) Asks the next probing question using the dynamic Dark Archangel rules.
     * B) Terminates the interview (if `maxQuestions` is reached) and synthesizes the entire log
     *    into a final `extendedStory` to save in the database.
     * 
     * @param request The chat history payload from the client.
     * @param maxQuestions The maximum number of questions the Archangel is allowed to ask.
     * @return ChatResponse containing the Archangel's reply and a flag indicating if it's the final turn.
     */
    public ChatResponse interactiveChat(ChatRequest request, int maxQuestions) {
        int angelMessageCount = 0;
        StringBuilder historyBuilder = new StringBuilder();
        for (ChatRequest.Message m : request.history) {
            historyBuilder.append(m.role.toUpperCase()).append(": ").append(m.text).append("\n");
            if ("angel".equalsIgnoreCase(m.role)) angelMessageCount++;
        }

        ChatResponse response = new ChatResponse();

        if (angelMessageCount >= maxQuestions) {
            // Reached max questions, time to compile into a confession
            String prompt = "You are a factual database archivist compiling a final confession record.\n" +
                    "Review this interrogation transcript:\n" + historyBuilder.toString() + "\n" +
                    "Write a highly dense, factual, and detailed 'extended story' paragraph. " +
                    "NO poetic fluff, NO flowery words. DO NOT fluff up a short story. " +
                    "Include the precise timeline of the incident, the emotional state, " +
                    "and every single concrete detail probed during the interrogation. " +
                    "CRITICAL RULE: NEVER mention the confessor's name, any relatives' names, or any exact geographical names/places (replace them with generic descriptions like 'a city park' or 'a male software engineer'). " +
                    "After the story, append a new line with 'Persona: [Age Group], [Gender], [Occupation/Status]'. " +
                    "OMIT the exact name and exact age, use an age group (e.g., 'Late 20s'). " +
                    "This story will be used to generate physical clues, so every concrete detail matters. " +
                    "Return ONLY the story paragraph and Persona line.";
            String story = aiClient.generateContentLight(prompt);
            
            Confession c = new Confession();
            c.setText(request.history.get(0).text); // the original first message
            c.setExtendedStory(story != null ? story.trim() : c.getText());
            c.setSpicy(true);
            if (request.locationName != null) c.setLocationName(request.locationName);
            if (request.placeType != null) c.setPlaceType(request.placeType);
            confessionRepository.save(c);

            response.reply = ("Hinglish".equalsIgnoreCase(request.language)) ? "Tumhara paap darj ho gaya hai." : "Your sin is recorded. The scales of judgment have tipped.";
            response.isFinal = true;
            return response;
        } else {
            // Ask the next question
            String languageRule = "Hinglish".equalsIgnoreCase(request.language) 
                ? "You MUST speak in Hinglish (Hindi + English typed in Latin alphabet)." 
                : "You MUST use brilliant, godly, and powerful English words. Sound like a terrifying deity.";
            
            String customRules = getPromptConfig("dark_angel_rules", "Be brutally analytical. Ask piercing questions.");
            String focals = getPromptConfig("dark_angel_focals", "1. Demographics (Age, Gender, City) 2. Deep Root Cause of the sin 3. Emotional State and Guilt Level");
            
            String systemPrompt = "You are the Dark Archangel, interrogating a mortal who is confessing a secret.\n" +
                    "You are brilliant, godly, powerful, and terrifying.\n" +
                    "Your focals to extract during this interrogation are:\n" + focals + "\n\n" +
                    "Custom Rules:\n" + customRules + "\n\n" +
                    "STRICT RULES: " + languageRule + "\n" +
                    "Keep it strictly to 1 or 2 short sentences. Ask ONE profound, probing question.";
            
            String userPrompt = "Here is the dialogue so far:\n" + historyBuilder.toString() + "\n" +
                    "TASK: Reply with your next question or godly observation.";
            
            String reply = aiClient.generateContentLight(systemPrompt + "\n" + userPrompt);
            
            // GUARDRAIL
            if (reply != null) {
                String guardrailSystem = "You are a guardrail model. You ensure safety and relevance.";
                String guardrailUser = "Check this output from the Dark Archangel: '" + reply + "'\n" +
                        "Does it use banned/highly offensive words or is it completely unrelated to the confession transcript below?\n" +
                        "Transcript: " + historyBuilder.toString() + "\n" +
                        "If it violates rules, reply with exactly 'FAIL'. Otherwise, reply 'PASS'.";
                String guardResult = aiClient.generateContentLight(guardrailSystem + "\n" + guardrailUser);
                
                if (guardResult != null && guardResult.contains("FAIL")) {
                    System.out.println("Guardrail caught bad output. Retrying exactly once.");
                    reply = aiClient.generateContentLight(systemPrompt + "\n" + userPrompt); // retry once
                }
            }
            
            response.reply = reply != null ? reply.trim() : "Speak. I am waiting.";
            response.isFinal = false;
            return response;
        }
    }
}
