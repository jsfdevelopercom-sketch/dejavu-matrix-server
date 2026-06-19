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
                    "Read the confession. Generate 35 precise, leading questions to extract dense, concrete, hyper-detailed data covering the PAST (deep psychological background, hidden motives, long-term prior events), " +
                    "the PRESENT (the exact minute-by-minute timeline, visceral physical actions, vivid locations, dialogue, sensory details, tools used during the event), " +
                    "and the FUTURE (quantifiable consequences, elaborate cover-ups, emotional fallout, police investigations). Ensure the story has a massive hook, extreme suspense, and a mind-blowing satisfying conclusion.";
            String probeUser = "Confession: \"" + confession.getText() + "\"";
            String questions = aiClient.generateContentLight(probeSystem + "\n" + probeUser);

            // 2. Generate persona and answer the questions
            String personaSystem = "You are the confessor answering questions with EXTREMELY DENSE, CONCRETE FACTS and VIVID SENSORY DETAILS. " +
                    "Do not use generic storytelling tropes. Provide highly specific demographics, precise dates, exact times, currency amounts, and minute-by-minute actions. " +
                    "Paint a highly detailed picture of the environment, the psychology, and the raw events. Ensure the answers build an incredibly rich, dark, complex story arc with intense suspense and an unforgettable twist conclusion.";
            String personaUser = "Confession: \"" + confession.getText() + "\"\nQuestions:\n" + questions;
            String answers = aiClient.generateContentLight(personaSystem + "\n" + personaUser);

            // IMPORTANT: Save the newly generated dense extended story back to the confession
            confession.setExtendedStory(answers);
            confessionRepository.save(confession);

            // 3. Generate structured JSON game content
            String jsonSystem = "You are Aarcus, an Expert Hollywood level story writer for psychological thriller mystery films. " +
                    "Read the highly detailed raw data and output a strict JSON structure containing the game content. " +
                    "CRITICAL REQUIREMENT: Use the content to create a HIGH adrenaline, extremely detailed story. Output VIVID, immersive, factual content with a powerful hook, intense suspense, and a massive, mind-bending TWIST at the end. " +
                    "RULES:\n" +
                    "- title: 2-4 word gripping title.\n" +
                    "- demographics: A JSON object containing deeply specific { age, occupation, gender, maritalStatus, locationType }.\n" +
                    "- motive: 2-4 sentence deep psychological description of the hidden motive.\n" +
                    "- emotionalSignificance: 2-4 sentence vivid description of the psychological impact of this event.\n" +
                    "- fullRevealText: 100-200 words, vivid, highly detailed timeline of events. Must have a gripping hook, sensory details, and an explosive conclusion.\n" +
                    "- anonymizedSummary: 1-2 factual gripping sentences.\n" +
                    "- qualityScore: 0.0 to 5.0 (must be >= 3.0 to be playable).\n" +
                    "- fragments: Array of EXACTLY 10 fragments. Order them chronologically. The last fragment MUST be the finishing piece of the story, a MASSIVE TWIST. DO NOT INCLUDE ANY EXPLANATION OR ENDER LINE IN THE LAST FRAGMENT! It MUST purely be the final raw action/event of the story.\n" +
                    "  Each fragment must have:\n" +
                    "    - emotionFamily (Choose from: Sorrow, Fear, Anger, Guilt, Love, Relief)\n" +
                    "    - emotionShade (e.g. 'regret', 'panic', 'obsession')\n" +
                    "    - intensity (1-9 integer)\n" +
                    "    - fragmentText (15-25 words MAX. DENSE, VIVID, SHOCKING DETAILS. Paint a very clear picture of what is happening.)\n" +
                    "    - shortFragmentText (2-5 words for card center)\n" +
                    "    - judgments: Array of exactly 3 judgment questions (binary true/false) about the confessor based on this fragment and prior ones.\n" +
                    "      Each judgment must have:\n" +
                    "        - text (e.g., 'This person acts alone.')\n" +
                    "        - correctAnswer (true or false)\n" +
                    "        - difficulty ('EASY', 'MEDIUM', 'HARD')\n" +
                    "        - explanationForBackendOnly (1 factual reason)\n" +
                    "        - emotionalAxis (e.g., 'Responsibility', 'Desire')\n" +
                    "OUTPUT ONLY VALID JSON.";
            
            String jsonUser = "Confession: \"" + confession.getText() + "\"\nExpanded Details:\n" + answers;

            // Use Heavy model for structured JSON with retry loop
            for (int attempt = 1; attempt <= 3; attempt++) {
                try {
                    String rawJson = aiClient.generateContentHeavy(jsonSystem + "\n" + jsonUser);
                    
                    if (rawJson == null) {
                        System.err.println("Attempt " + attempt + " failed: rawJson is null.");
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
                    content.setQualityScore(root.path("qualityScore").asDouble());
                    content.setCreatedByModel("Gemini-Heavy-Game-Factory");
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
                } catch (Exception parseEx) {
                    System.err.println("Attempt " + attempt + " parsing failed: " + parseEx.getMessage());
                    try { Thread.sleep(5000); } catch (InterruptedException ignored) {}
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
