package com.dejavu.backend.ai;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * AI Grader that evaluates if a raw confession is actually playable
 * as a Deja-Vu game room, using a heavy AI model.
 */
@Component
public class ConfessionQualityGrader {

    @Autowired
    private OpenAiClient openAiClient;
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    public static class GradingResult {
        public Object scores; // map of scores
        public int totalScore;
        public int maxScore;
        public int percentage;
        public String decision;
        public String oneSentenceCoreTruth;
        public Object primaryThemes;
        public String detectedPlace;
        public String suggestedBackgroundType;
        public Object suggestedObjectSymbols;
        public String mainProblem;
        public String gradingNotes;
        public boolean isSpicy;
    }

    public GradingResult gradeConfession(String confessionText) {
        String prompt = "You are an elite, formal Writing Professor and Story Grader for Deja-vu, a dark confession guessing game.\n" +
            "Your job is to grade ONE anonymous confession and rigorously decide whether it is suitable for conversion into a playable room.\n" +
            "You must grade it like a master's level fiction workshop. We do NOT want childish pranks, mundane events, or generic tropes.\n" +
            "Grade each parameter from 0 to 10:\n" +
            "1. Narrative Conflict: Is there a profound internal or external struggle?\n" +
            "2. Stakes & Consequence: Does the confession carry genuine, life-altering weight? Is something permanent lost or gained?\n" +
            "3. Emotional Resonance: Does it evoke a visceral, raw human emotion (deep guilt, forbidden lust, terror, profound grief) rather than a cheap thrill?\n" +
            "4. Psychological Complexity: Does the motive reveal multi-layered human flaws rather than cartoonish evil or simple mistakes?\n" +
            "5. Moral Ambiguity: Does it operate in a compelling grey area?\n" +
            "6. Guessability: Is there a concrete action that a player could realistically guess through clues?\n" +
            "7. Show, Don't Tell Potential: Can this be translated into rich abstract metaphors and visual symbols?\n" +
            "Important:\n" +
            "- A confession like 'I let my friend take the fall for a prank' is childish. It has low stakes, low emotional resonance, and low psychological complexity. SCORE IT LOW AND REJECT IT.\n" +
            "- If the total score is under 50/70, the decision MUST be NOT_PLAYABLE or REJECT.\n" +
            "- Evaluate if the confession is 'Spicy' (possesses intense adult emotional depth, dangerous secrets, forbidden desires, or profound betrayal). Set isSpicy to true if it is.\n" +
            "- CRITICAL LIMITS: Limit output time and tokens. Output MUST be short and fast.\n" +
            "Return ONLY valid JSON in this exact structure:\n" +
            "{\n" +
            "  \"scores\": {\n" +
            "    \"narrativeConflict\": 0,\n" +
            "    \"stakesAndConsequence\": 0,\n" +
            "    \"emotionalResonance\": 0,\n" +
            "    \"psychologicalComplexity\": 0,\n" +
            "    \"moralAmbiguity\": 0,\n" +
            "    \"guessability\": 0,\n" +
            "    \"showDontTellPotential\": 0\n" +
            "  },\n" +
            "  \"totalScore\": 0,\n" +
            "  \"maxScore\": 70,\n" +
            "  \"percentage\": 0,\n" +
            "  \"decision\": \"PLAYABLE_HIGH_PRIORITY | PLAYABLE | WEAK_PLAYABLE_NEEDS_REWRITE | REJECT\",\n" +
            "  \"oneSentenceCoreTruth\": \"\",\n" +
            "  \"primaryThemes\": [],\n" +
            "  \"detectedPlace\": \"unknown\",\n" +
            "  \"suggestedBackgroundType\": \"\",\n" +
            "  \"suggestedObjectSymbols\": [],\n" +
            "  \"mainProblem\": \"\",\n" +
            "  \"gradingNotes\": \"\",\n" +
            "  \"isSpicy\": false\n" +
            "}\n" +
            "Confession to grade:\n" +
            "\"" + confessionText + "\"";

        String response = openAiClient.generateContent(prompt);
        if (response != null) {
            try {
                response = response.replaceAll("^```json\\s*", "").replaceAll("```\\s*$", "").trim();
                return objectMapper.readValue(response, GradingResult.class);
            } catch (Exception e) {
                System.err.println("Failed to parse AI grade: " + e.getMessage());
            }
        }
        return null;
    }
}
