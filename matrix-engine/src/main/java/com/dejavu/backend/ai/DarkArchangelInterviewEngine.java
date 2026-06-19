package com.dejavu.backend.ai;

import com.dejavu.backend.model.Confession;
import com.dejavu.backend.model.game.ConfessionFragment;
import com.dejavu.backend.model.game.ConfessionGameContent;
import com.dejavu.backend.model.game.JudgmentQuestion;
import com.dejavu.backend.repository.ConfessionRepository;
import com.dejavu.backend.repository.game.ConfessionGameContentRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DarkArchangelInterviewEngine {

    @Autowired
    private GeminiAiClient aiClient;

    @Autowired
    private ConfessionRepository confessionRepository;

    @Autowired
    private ConfessionGameContentRepository gameContentRepository;

    private final ObjectMapper mapper = new ObjectMapper();

    public void interviewAndExpand(Confession confession, int maxQuestions) {
        try {
            String probeSystem = "You are a ruthless, analytical data-miner extracting a completely exhaustive history. Read the confession. Generate 10 precise, leading questions.";
            String probeUser = "Confession: \"" + confession.getText() + "\"";
            String questions = aiClient.generateContentLight(probeSystem + "\n" + probeUser);

            String personaSystem = "You are the confessor answering questions with DENSE, CONCRETE FACTS ONLY.";
            String personaUser = "Confession: \"" + confession.getText() + "\"\nQuestions:\n" + questions;
            String answers = aiClient.generateContentLight(personaSystem + "\n" + personaUser);

            confession.setExtendedStory(answers);
            confessionRepository.save(confession);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public ConfessionGameContent generateGameContent(Confession confession) {
        try {
            // 1. Initial Probe - Ask leading questions to build the history
            String probeSystem = "You are a ruthless, analytical data-miner extracting a completely exhaustive history. " +
                    "Read the confession. Generate 15 precise, leading questions to extract dense, concrete data covering the PAST (background, motives, prior events), " +
                    "the PRESENT (the exact timeline, physical actions, locations, tools used during the event), " +
                    "and the FUTURE (quantifiable consequences, cover-ups, fallout). Do NOT ask about feelings or narrative. Only hard, factual data.";
            String probeUser = "Confession: \"" + confession.getText() + "\"";
            String questions = aiClient.generateContentLight(probeSystem + "\n" + probeUser);

            // 2. Generate persona and answer the questions
            String personaSystem = "You are the confessor answering questions with DENSE, CONCRETE FACTS ONLY. " +
                    "Zero storytelling, zero fluff, zero emotion. Provide highly specific demographics, dates, times, amounts, and exact actions. " +
                    "Use bullet points or short factual sentences.";
            String personaUser = "Confession: \"" + confession.getText() + "\"\nQuestions:\n" + questions;
            String answers = aiClient.generateContentLight(personaSystem + "\n" + personaUser);

            // 3. Generate structured JSON game content
            String jsonSystem = "You are the ArchangelEngine Data Compiler for the 'Confession Card Battle' game. " +
                    "Read the raw data and output a strict JSON structure containing the game content. " +
                    "CRITICAL REQUIREMENT: Use the content to create the story in SIMPLE language. Output DENSE, factual content. No word inflation. No flowery or complex vocabulary. Keep sentences straightforward and direct. " +
                    "RULES:\n" +
                    "- title: 2-4 word factual title.\n" +
                    "- demographics: A JSON object containing strictly extracted or heavily inferred { age, occupation, gender, maritalStatus, locationType }.\n" +
                    "- fullRevealText: 40-80 words, strictly factual, timeline of events, no fluff.\n" +
                    "- anonymizedSummary: 1 factual sentence.\n" +
                    "- qualityScore: 0.0 to 5.0 (must be >= 3.0 to be playable).\n" +
                    "- fragments: Array of exactly 6 to 10 fragments. Order them chronologically (Situation, Context, Action, Denial, Consequence, Hidden Motive).\n" +
                    "  Each fragment must have:\n" +
                    "    - emotionFamily (Choose from: Sorrow, Fear, Anger, Guilt, Love, Relief)\n" +
                    "    - emotionShade (e.g. 'regret', 'panic')\n" +
                    "    - intensity (1-9 integer)\n" +
                    "    - fragmentText (6-15 words MAX. DENSE FACTS ONLY. Simple, basic language.)\n" +
                    "    - shortFragmentText (2-4 words for card center)\n" +
                    "    - judgments: Array of exactly 3 judgment questions (binary true/false) about the confessor based on this fragment and prior ones.\n" +
                    "      Each judgment must have:\n" +
                    "        - text (e.g., 'This person acts alone.')\n" +
                    "        - correctAnswer (true or false)\n" +
                    "        - difficulty ('EASY', 'MEDIUM', 'HARD')\n" +
                    "        - explanationForBackendOnly (1 factual reason)\n" +
                    "        - emotionalAxis (e.g., 'Responsibility', 'Desire')\n" +
                    "OUTPUT ONLY VALID JSON.";
            
            String jsonUser = "Confession: \"" + confession.getText() + "\"\nExpanded Details:\n" + answers;

            // Use Heavy model for structured JSON
            String rawJson = aiClient.generateContentHeavy(jsonSystem + "\n" + jsonUser);
            
            // Clean up JSON if wrapped in markdown
            if (rawJson != null) {
                if (rawJson.startsWith("```json")) {
                    rawJson = rawJson.substring(7, rawJson.lastIndexOf("```")).trim();
                } else if (rawJson.startsWith("```")) {
                    rawJson = rawJson.substring(3, rawJson.lastIndexOf("```")).trim();
                }

                JsonNode root = mapper.readTree(rawJson);
                
                ConfessionGameContent content = new ConfessionGameContent();
                content.setConfessionId(confession.getId());
                content.setTitle(root.path("title").asText());
                content.setDemographics(root.path("demographics").toString()); // Assuming there's a setDemographics method
                content.setFullRevealText(root.path("fullRevealText").asText());
                content.setAnonymizedSummary(root.path("anonymizedSummary").asText());
                content.setQualityScore(root.path("qualityScore").asDouble());
                content.setCreatedByModel("Gemini-Heavy-Game-Factory");
                content.setStatus(content.getQualityScore() >= 3.0 ? "PLAYABLE" : "REJECTED");

                JsonNode frags = root.path("fragments");
                int order = 1;
                for (JsonNode fNode : frags) {
                    ConfessionFragment fragment = new ConfessionFragment();
                    fragment.setGameContent(content);
                    fragment.setFragmentOrder(order++);
                    fragment.setEmotionFamily(fNode.path("emotionFamily").asText());
                    fragment.setEmotionShade(fNode.path("emotionShade").asText());
                    fragment.setIntensity(fNode.path("intensity").asInt());
                    fragment.setFragmentText(fNode.path("fragmentText").asText());
                    fragment.setShortFragmentText(fNode.path("shortFragmentText").asText());
                    fragment.setFullFragmentText(fNode.path("fragmentText").asText()); // fallback
                    
                    JsonNode judgs = fNode.path("judgments");
                    for (JsonNode jNode : judgs) {
                        JudgmentQuestion jq = new JudgmentQuestion();
                        jq.setFragment(fragment);
                        jq.setText(jNode.path("text").asText());
                        jq.setCorrectAnswer(jNode.path("correctAnswer").asBoolean());
                        jq.setDifficulty(jNode.path("difficulty").asText());
                        jq.setExplanationForBackendOnly(jNode.path("explanationForBackendOnly").asText());
                        jq.setEmotionalAxis(jNode.path("emotionalAxis").asText());
                        fragment.getJudgments().add(jq);
                    }
                    content.getFragments().add(fragment);
                }

                return gameContentRepository.save(content);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // Keep interactiveChat for legacy Deja-Lord confession intake if needed, but simplified
    public ChatResponse interactiveChat(ChatRequest request, int maxQuestions) {
        ChatResponse response = new ChatResponse();
        response.reply = "Your sin is recorded. The scales of judgment have tipped.";
        response.isFinal = true;
        
        Confession c = new Confession();
        c.setText(request.history.get(0).text);
        c.setExtendedStory(c.getText());
        confessionRepository.save(c);
        
        // Asynchronously generate game content
        new Thread(() -> generateGameContent(c)).start();
        
        return response;
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
}
