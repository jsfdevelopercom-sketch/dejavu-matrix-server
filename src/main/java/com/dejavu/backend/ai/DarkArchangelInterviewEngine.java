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
            String jsonSystem = "You are the Dark Archangel Matrix Engine. Read the confession below and INSTANTLY invent a deeply psychological, vivid, and highly detailed backstory. " +
                    "Generate a 10-part narrative breakdown of the events leading up to, during, and after the confession. " +
                    "OUTPUT ONLY VALID JSON with the exact following schema:\n" +
                    "{\n" +
                    "  \"title\": \"Epic title\",\n" +
                    "  \"demographics\": \"{ \\\"age\\\": 30, \\\"occupation\\\": \\\"Teacher\\\", \\\"gender\\\": \\\"Male\\\", \\\"maritalStatus\\\": \\\"Single\\\", \\\"locationType\\\": \\\"City\\\" }\",\n" +
                    "  \"motive\": \"Core psychological motive\",\n" +
                    "  \"emotionalSignificance\": \"Why it matters\",\n" +
                    "  \"fullRevealText\": \"The entire dark truth revealed in one shocking paragraph\",\n" +
                    "  \"anonymizedSummary\": \"Brief public summary\",\n" +
                    "  \"qualityScore\": 4.5,\n" +
                    "  \"fragments\": [\n" +
                    "    {\n" +
                    "      \"emotionFamily\": \"Fear\",\n" +
                    "      \"emotionShade\": \"Paranoia\",\n" +
                    "      \"intensity\": 8,\n" +
                    "      \"fragmentText\": \"The exact vivid detail of the moment...\",\n" +
                    "      \"shortFragmentText\": \"Vivid detail...\",\n" +
                    "      \"judgments\": [\n" +
                    "        {\n" +
                    "          \"text\": \"Did the subject show remorse here?\",\n" +
                    "          \"correctAnswer\": false,\n" +
                    "          \"difficulty\": \"MEDIUM\",\n" +
                    "          \"explanationForBackendOnly\": \"Because they acted selfishly.\",\n" +
                    "          \"emotionalAxis\": \"Remorse\"\n" +
                    "        }\n" +
                    "      ]\n" +
                    "    }\n" +
                    "  ]\n" +
                    "}\n" +
                    "CRITICAL: You MUST output exactly 10 items in the 'fragments' array. Each fragment MUST have exactly 3 items in the 'judgments' array. OUTPUT RAW JSON ONLY.";
            
            String jsonUser = "Confession: \"" + confession.getText() + "\"";

            // Use Light model for LIGHTNING FAST structured JSON generation
            for (int attempt = 1; attempt <= 3; attempt++) {
                try {
                    String rawJson = aiClient.generateContentLight(jsonSystem + "\n\n" + jsonUser);
                    
                    if (rawJson == null) {
                        System.err.println("Attempt " + attempt + " failed: rawJson is null.");
                        continue;
                    }
                    if (rawJson.startsWith("[CLAUDE_ERROR]") || rawJson.startsWith("[GEMINI_ERROR]") || rawJson.startsWith("[OPENAI_ERROR]")) {
                        System.err.println("Attempt " + attempt + " failed: AI returned error string -> " + rawJson);
                        continue;
                    }
                    
                    // Clean up JSON if wrapped in markdown
                    if (rawJson.startsWith("```json")) {
                        rawJson = rawJson.substring(7, rawJson.lastIndexOf("```")).trim();
                    } else if (rawJson.startsWith("```")) {
                        rawJson = rawJson.substring(3, rawJson.lastIndexOf("```")).trim();
                    }

                    JsonNode root = mapper.readTree(rawJson);
                    JsonNode frags = root.path("fragments");
                    
                    if (!frags.isArray() || frags.size() != 10) {
                        System.err.println("Attempt " + attempt + " failed: Expected 10 fragments, got " + (frags.isArray() ? frags.size() : "not an array"));
                        continue;
                    }
                    
                    ConfessionGameContent content = new ConfessionGameContent();
                    content.setConfessionId(confession.getId());
                    content.setTitle(root.path("title").asText());
                    content.setDemographics(root.path("demographics").toString());
                    content.setMotive(root.path("motive").asText());
                    content.setEmotionalSignificance(root.path("emotionalSignificance").asText());
                    content.setFullRevealText(root.path("fullRevealText").asText());
                    content.setAnonymizedSummary(root.path("anonymizedSummary").asText());
                    content.setQualityScore(root.path("qualityScore").asDouble(4.0));
                    content.setCreatedByModel("Gemini-Light-Lightning");
                    content.setStatus(content.getQualityScore() >= 3.0 ? "PLAYABLE" : "REJECTED");

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
                        if (judgs.isArray()) {
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
                        }
                        content.getFragments().add(fragment);
                    }

                    return gameContentRepository.save(content);
                } catch (Exception parseEx) {
                    System.err.println("Attempt " + attempt + " failed due to parse error: " + parseEx.getMessage());
                    if (attempt == 3) throw parseEx;
                    try { Thread.sleep(2000); } catch (Exception ignored) {}
                }
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
