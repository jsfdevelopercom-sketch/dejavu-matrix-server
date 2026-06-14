package com.dejavu.backend.ai;

import com.dejavu.backend.model.Confession;
import com.dejavu.backend.repository.ConfessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DarkArchangelInterviewEngine {

    @Autowired
    private OpenAiClient openAiClient; // Using GPT High Model as requested

    @Autowired
    private ConfessionRepository confessionRepository;

    /**
     * Converts a basic confession into a rich short story by running a true
     * multi-agent simulation between the Dark Archangel and a Mortal Persona.
     */
    public Confession interviewAndExpand(Confession confession, int maxQuestions) {
        try {
            // 1. Initialize Mortal Persona
            String personaSystem = "You are a psychological profiler.";
            String personaUser = "Read this confession: \"" + confession.getText() + "\"\n" +
                    "Generate a realistic persona for this mortal. Include age, gender, occupation, and psychological state. Keep it under 50 words.";
            String persona = openAiClient.generateContent(personaSystem, personaUser);
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
                
                String archangelQ = openAiClient.generateContent(archangelSystem, archangelUser);
                if (archangelQ == null) break;
                transcript.append("ARCHANGEL: ").append(archangelQ.trim()).append("\n");

                // Mortal's Turn
                String mortalSystem = "You are roleplaying as a mortal being interrogated by a terrifying Archangel.\n" +
                        "Your Persona: " + persona + "\n" +
                        "Do NOT break character. Keep it under 40 words.";
                String mortalUser = "Transcript so far:\n" + transcript.toString() + "\n" +
                        "TASK: Answer the Archangel's latest question in character. Reveal your demographics if asked, or refuse. " +
                        "Show emotion (guilt, defiance, fear).";
                
                String mortalA = openAiClient.generateContent(mortalSystem, mortalUser);
                if (mortalA == null) break;
                transcript.append("MORTAL: ").append(mortalA.trim()).append("\n\n");
            }

            // 3. Synthesize Final Story
            String synthesisSystem = "You are the Dark Archangel. Return ONLY the extended story paragraph. No prefix, no JSON. DO NOT use fake fluff, flowery, or poetic words. State the hard facts.";
            String synthesisUser = "Review this completed interrogation transcript:\n" +
                    transcript.toString() + "\n" +
                    "TASK: Synthesize this into a highly dense, factual, and detailed 'Extended Story' paragraph. " +
                    "Include the exact demographics, the precise timeline of the incident, the emotional state, " +
                    "and every single detail probed. This story will be used to generate physical clues.";
            
            String finalStory = openAiClient.generateContent(synthesisSystem, synthesisUser);
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
     * Generates a fast, dark, funny waiting text while the heavy model processes the real response.
     */
    public String generateFiller(ChatRequest request) {
        String lastUserText = request.history.get(request.history.size() - 1).text;
        String systemPrompt = "You are the Dark Archangel. The mortal just said: '" + lastUserText + "'. " +
                "You are currently thinking/weighing their soul. " +
                "Generate exactly ONE short, dark, yet funny 'filler' sentence to show you are thinking. " +
                "Do NOT use poetic words like 'shadows'. Be mocking and impatient. Example: 'Let me consult the ledger of your pathetic choices...'";
        
        String filler = openAiClient.generateContent(systemPrompt, "TASK: Generate thinking filler.");
        return filler != null ? filler.trim() : "The Archangel is weighing your pathetic words...";
    }

    @Autowired
    private com.dejavu.backend.repository.PromptConfigRepository promptConfigRepository;

    private String getPromptConfig(String key, String defaultValue) {
        return promptConfigRepository.findById(key)
            .map(com.dejavu.backend.model.PromptConfig::getPromptContent)
            .orElse(defaultValue);
    }

    /**
     * Handles a single turn of the interactive user confession interview.
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
            String prompt = "You are the Dark Archangel compiling a final confession record.\n" +
                    "Review this interrogation transcript:\n" + historyBuilder.toString() + "\n" +
                    "Write a highly dense, factual, and detailed 'extended story' paragraph. " +
                    "DO NOT use fake fluff, flowery, or poetic words. State the facts. " +
                    "Include the exact demographics, the precise timeline of the incident, the emotional state, " +
                    "and every single detail probed during the interrogation. " +
                    "This story will be used to generate physical clues, so every detail matters. " +
                    "Return ONLY the story paragraph.";
            String story = openAiClient.generateContent(prompt);
            
            Confession c = new Confession();
            c.setText(request.history.get(0).text); // the original first message
            c.setExtendedStory(story != null ? story.trim() : c.getText());
            c.setSpicy(true);
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
            
            String reply = openAiClient.generateContent(systemPrompt, userPrompt);
            
            // GUARDRAIL
            if (reply != null) {
                String guardrailSystem = "You are a guardrail model. You ensure safety and relevance.";
                String guardrailUser = "Check this output from the Dark Archangel: '" + reply + "'\n" +
                        "Does it use banned/highly offensive words or is it completely unrelated to the confession transcript below?\n" +
                        "Transcript: " + historyBuilder.toString() + "\n" +
                        "If it violates rules, reply with exactly 'FAIL'. Otherwise, reply 'PASS'.";
                String guardResult = openAiClient.generateContent(guardrailSystem, guardrailUser);
                
                if (guardResult != null && guardResult.contains("FAIL")) {
                    System.out.println("Guardrail caught bad output. Retrying exactly once.");
                    reply = openAiClient.generateContent(systemPrompt, userPrompt); // retry once
                }
            }
            
            response.reply = reply != null ? reply.trim() : "Speak. I am waiting.";
            response.isFinal = false;
            return response;
        }
    }
}
