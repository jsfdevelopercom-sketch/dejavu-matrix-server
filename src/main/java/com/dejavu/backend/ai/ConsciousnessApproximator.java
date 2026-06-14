package com.dejavu.backend.ai;

import com.dejavu.backend.model.MatrixHuman;
import org.springframework.stereotype.Component;

@Component
public class ConsciousnessApproximator {

    private final GeminiAiClient aiClient;

    public ConsciousnessApproximator(GeminiAiClient aiClient) {
        this.aiClient = aiClient;
    }

    /**
     * Approximates consciousness by taking the previous working memory and
     * the new raw events of the day, synthesizing them strictly in the FIRST PERSON,
     * and maintaining a capped "working memory" state to represent current attention.
     */
    public String synthesizeFirstPersonConsciousness(MatrixHuman human, String newDayEvents) {
        String systemPrompt = "You are a Consciousness Approximator. You must output the internal monologue of the human reflecting on their day. " +
                "You MUST write strictly in the FIRST PERSON ('I woke up', 'I felt', 'I thought'). " +
                "Do not use third person. Focus on their subjective experience, emotions, and immediate working memory.";

        String currentWorkingMemory = human.getWorkingMemory() != null ? human.getWorkingMemory() : "No active thoughts.";

        String userPrompt = "Subject Name: " + human.getName() + "\n" +
                "Personality: " + human.getPersonality() + "\n" +
                "Current Working Memory (recent thoughts): " + currentWorkingMemory + "\n" +
                "New Events Today: " + newDayEvents + "\n\n" +
                "TASK: Synthesize these events into your internal stream of consciousness for today. Keep it to one paragraph. Write strictly as " + human.getName() + " in the first person.";

        String newStreamOfConsciousness = aiClient.generateContentLight(systemPrompt + "\n\n" + userPrompt);

        if (newStreamOfConsciousness != null) {
            String updatedWorkingMemory = currentWorkingMemory + "\n" + newStreamOfConsciousness.trim();
            // Cap working memory at ~2000 characters to simulate limited attention span
            if (updatedWorkingMemory.length() > 2000) {
                updatedWorkingMemory = updatedWorkingMemory.substring(updatedWorkingMemory.length() - 2000);
            }
            human.setWorkingMemory(updatedWorkingMemory);
            return newStreamOfConsciousness.trim();
        }

        return newDayEvents; // Fallback if AI fails
    }
}
