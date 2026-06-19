package com.dejavu.backend.ai;

import com.dejavu.backend.model.MatrixHuman;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MatrixMindEngine {

    @Autowired
    private OpenAiClient openAiClient;

    @Autowired
    private GeminiAiClient geminiAiClient;

    private static final String MODEL_NANO = "gpt-5.4-nano";
    private static final String MODEL_MINI = "gpt-5.4-mini";
    private static final String MODEL_HEAVY = "gpt-4o";

    /**
     * The Consciousness Engine actively perceives raw external events and formats them 
     * into a subjective first-person reality stream.
     */
    public String perceiveEvent(MatrixHuman human, String rawEvent) {
        String systemPrompt = "You are the Consciousness Engine for " + human.getName() + ".\n" +
                "You are experiencing a stream of raw events. IMPORTANT: Review your LTM first. If you have recently experienced severe trauma or life-altering events, your perception of current mundane events MUST be heavily colored by it (e.g., hypervigilance, dissociation, lingering fear).\n" +
                "FIRST, write a 1-sentence 'SYNOPSIS' of your overarching mental state based on all your memory.\n" +
                "THEN, translate the raw events strictly into what you SEE, HEAR, and FEEL physically in the first person.\n" +
                "Format as:\nSYNOPSIS: ...\nPERCEPTION: ...";
        
        String userPrompt = "LTM (Core Memory): " + human.getMemory() + "\nSTM Context: " + human.getWorkingMemory() + "\nRaw Event: " + rawEvent;
        
        String perception = openAiClient.generateContent(systemPrompt, userPrompt, MODEL_HEAVY);
        return perception != null ? perception.trim() : rawEvent;
    }

    /**
     * The Emotions Engine takes the perceived reality and generates an emotional response based on personality and LTM.
     */
    public String feelEmotion(MatrixHuman human, String perception) {
        String systemPrompt = "You are the Emotions Engine (Amygdala/Limbic system) for " + human.getName() + ".\n" +
                "Read the subjective perception of the current moment. Based on your personality and long-term memory, " +
                "how does this make you FEEL? Output only your internal emotional state and visceral bodily reactions in the first person. 1-2 sentences.";
        
        String userPrompt = "Personality: " + human.getPersonality() + "\nLTM: " + human.getMemory() + "\n\nPerception: " + perception;
        
        // Use Gemini for variety in the emotional engine
        String emotion = geminiAiClient.generateContentLight(systemPrompt + "\n\n" + userPrompt);
        return emotion != null ? emotion.trim() : "I feel numb.";
    }

    /**
     * The Thinking Engine (Prefrontal Cortex) takes perception, emotion, and memory, and generates a higher-level thought or action plan.
     */
    public String generateThought(MatrixHuman human, String perception, String emotion) {
        String systemPrompt = "You are the Thinking Engine (Higher Cortex) for " + human.getName() + ".\n" +
                "You must synthesize your overarching mental state (from Perception), your emotional state, and your core memories into a concrete thought or action plan.\n" +
                "Do not ignore major past traumas. If you were recently hurt, you will not casually focus on mundane things like drinking chai.\n" +
                "Write in the first person. What do you conclude? What will you do next? Keep it grounded and realistic. 2-3 sentences.";
        
        String userPrompt = "Personality: " + human.getPersonality() + "\nSTM: " + human.getWorkingMemory() + "\nLTM: " + human.getMemory() + "\n\n" +
                "What I Perceive: " + perception + "\n" +
                "How I Feel: " + emotion;
        
        String thought = openAiClient.generateContent(systemPrompt, userPrompt, MODEL_HEAVY);
        return thought != null ? thought.trim() : "I don't know what to think.";
    }

    /**
     * Main entry point for the Mind to process a new event.
     * Returns the finalized internal monologue to be saved to STM.
     */
    public String processEventToMind(MatrixHuman human, String rawEvent) {
        String perception = perceiveEvent(human, rawEvent);
        String emotion = feelEmotion(human, perception);
        String thought = generateThought(human, perception, emotion);
        
        return "[Perception]: " + perception + "\n[Emotion]: " + emotion + "\n[Thought]: " + thought;
    }
}
