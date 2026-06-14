package com.dejavu.backend.ai;

import com.dejavu.backend.model.Confession;
import com.dejavu.backend.model.RoomBlueprint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.util.ArrayList;
import java.util.List;

@Component
public class AiRoomBlueprintGenerator {

    @Autowired
    private OpenAiClient openAiClient;
    
    @Autowired
    private FallbackRoomBlueprintGenerator fallbackGenerator;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public RoomBlueprint generate(Confession confession, String language) {
        try {
            String fullStory = (confession.getExtendedStory() != null && !confession.getExtendedStory().trim().isEmpty()) 
                    ? confession.getExtendedStory() 
                    : confession.getText();
                    
            String prompt1 = "You are an ancient, extremely intelligent (IQ 200+), brutal, mocking, and jeering entity running a psychological deduction game. Read this confession story:\n" +
                    "\"" + fullStory + "\"\n\n" +
                    "STRICT PERSONA RULES: DO NOT use poetic, cheesy, or 'horror' words like 'dark', 'shadows', 'whispers'. Speak with cold, brutal, calculating intelligence. Mock the user's feeble mind subtly. Clues must be short (1-2 lines, 6-12 words max), brutal, and simple.\n\n" +
                    "GAME DESIGN RULES:\n" +
                    "1. TARGET TRUTH FRAME: Classify the confession into relationship, actionCategory, victimType, place, emotion, consequence.\n" +
                    "2. FORBIDDEN WORDS (TABOO): List 5-8 obvious giveaway words from the confession. NEVER use these in clues.\n" +
                    "3. PROGRESSIVE ESCALATION (CLUE LADDER):\n" +
                    "   - Clue 1 (20-35% guessability): Emotion + broad theme only. Do not reveal the exact act.\n" +
                    "   - Clue 2 (35-55% guessability): Context and Relationship. Use the 'Triangulation' rule (describe physical evidence/circumstances, not the action).\n" +
                    "   - Clue 3 (55-75% guessability): Action category. What kind of betrayal or event was it?\n" +
                    "   - Clue 4 (75-90% guessability): Near-reveal without using forbidden words.\n" +
                    "4. DISCRIMINABILITY: A clue is BAD if it fits every confession. It must narrow the search space.\n" +
                    "5. SURFACE AND BASE: Each clue must have an internal explanation ('whyItFits').\n\n" +
                    "Return ONLY JSON matching this EXACT schema (Language: " + language + "):\n" +
                    "{\n" +
                    "  \"roomTitle\": \"...\",\n" +
                    "  \"detectedPlace\": \"...\",\n" +
                    "  \"primaryEmotion\": \"...\",\n" +
                    "  \"themes\": [\"...\"],\n" +
                    "  \"backgroundAssetId\": \"...\",\n" +
                    "  \"objectAssetIds\": [\"...\"],\n" +
                    "  \"openingAngelLine\": \"...\",\n" +
                    "  \"customScoringParams\": \"...\",\n" +
                    "  \"hiddenTargetSummary\": \"...\",\n" +
                    "  \"winAngelLine\": \"...\",\n" +
                    "  \"translatedConfession\": \"...\",\n" +
                    "  \"targetTruthFrame\": { \"relationship\": \"...\", \"actionCategory\": \"...\", \"victimType\": \"...\", \"emotion\": \"...\", \"consequence\": \"...\" },\n" +
                    "  \"forbiddenDirectWords\": [\"...\"],\n" +
                    "  \"clues\": [\n" +
                    "    { \"text\": \"clue 1 text\", \"whyItFits\": \"explanation\" },\n" +
                    "    { \"text\": \"clue 2 text\", \"whyItFits\": \"explanation\" },\n" +
                    "    { \"text\": \"clue 3 text\", \"whyItFits\": \"explanation\" },\n" +
                    "    { \"text\": \"clue 4 text\", \"whyItFits\": \"explanation\" }\n" +
                    "  ]\n" +
                    "}";

            String response1 = openAiClient.generateContent(prompt1);
            if (response1 == null) throw new IllegalStateException("Gemini returned null");
            response1 = response1.replaceAll("^```json\\s*", "").replaceAll("```\\s*$", "").trim();
            JsonNode root = objectMapper.readTree(response1);

            ArrayNode cluesArr = (ArrayNode) root.get("clues");
            String safeC1 = "You lack the capacity to understand.";
            String safeC2 = "Look closer, if your mind permits.";
            String safeC3 = "The truth is simpler than your guesses.";
            String safeC4 = "You are staring directly at it.";

            if (cluesArr != null && cluesArr.size() >= 4) {
                safeC1 = cluesArr.get(0).get("text").asText();
                safeC2 = cluesArr.get(1).get("text").asText();
                safeC3 = cluesArr.get(2).get("text").asText();
                safeC4 = cluesArr.get(3).get("text").asText();
            }

            RoomBlueprint bp = new RoomBlueprint();
            bp.setRoomTitle(root.has("roomTitle") ? root.get("roomTitle").asText() : "Unknown Room");
            bp.setDetectedPlace(root.has("detectedPlace") ? root.get("detectedPlace").asText() : "");
            bp.setPrimaryEmotion(root.has("primaryEmotion") ? root.get("primaryEmotion").asText() : "");
            
            List<String> themesList = new ArrayList<>();
            if (root.has("themes") && root.get("themes").isArray()) {
                for (JsonNode t : root.get("themes")) themesList.add(t.asText());
            }
            bp.setThemesList(themesList);
            
            bp.setBackgroundAssetId(root.has("backgroundAssetId") ? root.get("backgroundAssetId").asText() : "");
            
            List<String> objList = new ArrayList<>();
            if (root.has("objectAssetIds") && root.get("objectAssetIds").isArray()) {
                for (JsonNode t : root.get("objectAssetIds")) objList.add(t.asText());
            }
            bp.setObjectAssetIdsList(objList);
            
            bp.setOpeningAngelLine(root.has("openingAngelLine") ? root.get("openingAngelLine").asText() : "");
            bp.setCustomScoringParams(root.has("customScoringParams") ? root.get("customScoringParams").asText() : "");
            bp.setHiddenTargetSummary(root.has("hiddenTargetSummary") ? root.get("hiddenTargetSummary").asText() : "");
            bp.setWinAngelLine(root.has("winAngelLine") ? root.get("winAngelLine").asText() : "");
            bp.setTranslatedConfession(root.has("translatedConfession") ? root.get("translatedConfession").asText() : "");

            bp.setClue1(safeC1);
            bp.setClue2(safeC2);
            bp.setExtraClue3(safeC3);
            bp.setExtraClue4(safeC4);

            bp.setConfessionId(confession.getId());
            bp.setGeneratedByModel(true);
            return bp;

        } catch (Exception e) {
            System.err.println("Failed to parse or validate AI room blueprint: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.err.println("Using fallback blueprint generator.");
        return fallbackGenerator.generateFallback(confession.getId(), confession.getText());
    }
}
