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
                    
            String prompt1 = "You are a master forensic detective writing clues for a physical escape room based on a true confession.\n" +
                    "Read this confession story:\n" +
                    "\"" + fullStory + "\"\n\n" +
                    "CRITICAL CLUE WRITING RULES (VIOLATING THESE WILL BREAK THE GAME):\n" +
                    "1. NO ABSTRACTION: NEVER use abstract, philosophical, or 'summary' words like 'betrayal', 'impactful', 'simplicity', 'privilege', 'punishment', 'truth', 'secret', 'guilt'.\n" +
                    "2. PHYSICAL EVIDENCE ONLY: A clue MUST describe a physical object, a sensory detail, a specific location trait, or a concrete action. Treat the confession as a crime scene. What was left behind? What did the room look like? What was the exact physical act?\n" +
                    "3. FORBIDDEN WORDS (TABOO): Identify the 5 most obvious words in the confession. You CANNOT use them or their synonyms in any clue.\n" +
                    "4. PROGRESSIVE LADDER:\n" +
                    "   - Clue 1: Describe the physical setting or an associated object left behind at the scene. (e.g., instead of 'a secret betrayal', write 'A half-empty coffee cup next to an unlocked computer').\n" +
                    "   - Clue 2: Describe the relationship or victim using physical/sensory traits. (e.g., instead of 'a cheated friend', write 'Two matching friendship bracelets, one thrown in the trash').\n" +
                    "   - Clue 3: Describe the physical action taken without naming the crime. (e.g., 'The backspace key was hit thirty times to rewrite the timeline').\n" +
                    "   - Clue 4: The Near-Reveal. The most concrete piece of evidence that almost gives it away, strictly obeying the Taboo rule.\n" +
                    "5. Keep clues under 15 words. Sound factual and forensic, NOT poetic.\n\n" +
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
            
            int bgRand = new java.util.Random().nextInt(10) + 1;
            bp.setBackgroundAssetId(String.format("room_bg_%02d", bgRand));
            
            List<String> validObjs = java.util.Arrays.asList("obj_old_phone_01", "obj_crumpled_letter_01", "obj_broken_mirror_01", "obj_candle_01", "obj_music_box_01", "obj_rusty_key_01");
            List<String> objList = new ArrayList<>();
            java.util.Collections.shuffle(validObjs);
            for(int i=0; i<4; i++) {
                objList.add(validObjs.get(i));
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
