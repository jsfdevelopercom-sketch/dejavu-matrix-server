package com.dejavu.backend.ai;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

@Component
public class AiGuessScorer {

    @Autowired
    private OpenAiClient openAiClient;
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    public static class ScoreResult {
        public int accuracy;
        public String tier;
        public String angelFeedback;
        
        public ScoreResult() {}
        public ScoreResult(int accuracy, String tier, String angelFeedback) {
            this.accuracy = accuracy;
            this.tier = tier;
            this.angelFeedback = angelFeedback;
        }
    }

    @Autowired
    private com.dejavu.backend.repository.PromptConfigRepository promptConfigRepository;

    public ScoreResult scoreGuess(String guess, String targetSummary, String customScoringParams, String language) {
        String defaultRules = "- Speak like a real human standing uncomfortably close to the player. Make it deeply personal, intrusive, and dark.\n" +
                "- DO NOT be robotic. DO NOT be poetic or metaphorical. Use very easy, everyday words.\n" +
                "- STRICTLY ONE SINGLE SENTENCE for each field. Never use multiple sentences or run-ons.\n" +
                "- Evaluate the guess semantically against the custom scoring parameters. Do not require exact word matches.";

        String rules = promptConfigRepository.findById("dark_angel_rules")
                .map(com.dejavu.backend.model.PromptConfig::getPromptContent)
                .orElseGet(() -> {
                    com.dejavu.backend.model.PromptConfig pc = new com.dejavu.backend.model.PromptConfig("dark_angel_rules", defaultRules);
                    promptConfigRepository.save(pc);
                    return defaultRules;
                });

        String prompt = "You are an expert, rigid semantic evaluator for a high-stakes detective guessing game. Your role is to score a player's guess against a hidden truth.\n\n" +
                "The actual hidden confession summary is: \"" + targetSummary + "\".\n" +
                "The absolute strict scoring parameters (the required 'Truth Frame') are: \"" + customScoringParams + "\".\n\n" +
                "The player guessed: \"" + guess + "\".\n\n" +
                "EVALUATION RULES:\n" +
                "1. DO NOT require exact word matches. Look for semantic equivalence (e.g., 'boss' = 'manager').\n" +
                "2. DO require the specific elements from the scoring parameters. If the parameter says 'Victim: Sister', and they guess 'Friend', that is INCORRECT. Score it low.\n" +
                "3. If they guess vague things like 'did something bad to someone', score it < 20. They must hit the specific action and victim.\n" +
                "4. Calculate the score from 0 to 100 based strictly on how many of the custom scoring parameters they correctly identified.\n\n" +
                "EXAMPLES:\n" +
                "Example 1: Params: '1. Victim: Brother. 2. Action: Stole money.' | Guess: 'Robbed his sibling.' -> Score: 100, Tier: S\n" +
                "Example 2: Params: '1. Victim: Co-worker. 2. Action: Sabotaged presentation.' | Guess: 'Ruined a friend's work.' -> Score: 40, Tier: C (Friend is not co-worker, missed presentation)\n" +
                "Example 3: Params: '1. Victim: Wife. 2. Action: Cheated.' | Guess: 'Did a bad thing.' -> Score: 0, Tier: F\n\n" +
                "Return exactly this JSON schema and nothing else:\n" +
                "{\n" +
                "  \"accuracy\": 85, // integer score 0-100\n" +
                "  \"tier\": \"<S for 90-100, A for 75-89, B for 50-74, C for 25-49, F for 0-24>\",\n" +
                "  \"angelFeedback\": \"A single, blunt sentence commenting on their guess. Apply these rules:\\n" + rules.replace("\"", "\\\"").replace("\n", "\\n") + "\\nIT MUST BE WRITTEN IN THIS LANGUAGE: " + language + "\"\n" +
                "}";

        String response = openAiClient.generateContent(prompt);
        if (response != null) {
            try {
                response = response.replaceAll("^```json\\s*", "").replaceAll("```\\s*$", "").trim();
                return objectMapper.readValue(response, ScoreResult.class);
            } catch (Exception e) {
                System.err.println("Failed to parse AI score: " + e.getMessage());
            }
        }

        // Fallback simple hybrid/keyword matching
        return calculateFallbackScore(guess, targetSummary);
    }
    
    private ScoreResult calculateFallbackScore(String guess, String targetSummary) {
        if (guess == null || targetSummary == null) {
            return new ScoreResult(0, "MISS", "The sand is silent.");
        }
        
        String normGuess = guess.toLowerCase().replaceAll("[^a-z0-9\\s]", "");
        String normTarget = targetSummary.toLowerCase().replaceAll("[^a-z0-9\\s]", "");
        
        String[] targetWords = normTarget.split("\\s+");
        int matchCount = 0;
        int validWordsCount = 0;
        
        for (String word : targetWords) {
            if (word.length() > 3) {
                validWordsCount++;
                if (normGuess.contains(word)) {
                    matchCount++;
                }
            }
        }
        
        int accuracy = 0;
        if (validWordsCount > 0) {
            accuracy = (int) (((double) matchCount / validWordsCount) * 100);
        } else if (normGuess.equals(normTarget)) {
            accuracy = 100;
        }
        
        if (accuracy >= 80) {
            return new ScoreResult(accuracy, "STRONG", "You see the truth struggling to the surface.");
        } else if (accuracy >= 50) {
            return new ScoreResult(accuracy, "CLOSE", "You dance on the edge of a painful memory.");
        } else if (accuracy >= 20) {
            return new ScoreResult(accuracy, "WEAK", "A faint whisper in the dark, but mostly dust.");
        } else {
            return new ScoreResult(accuracy, "MISS", "The shadows do not recognize those words.");
        }
    }
}
