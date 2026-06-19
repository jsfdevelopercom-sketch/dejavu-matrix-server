package com.dejavu.backend.ai.agent;
import com.dejavu.backend.ai.OpenAiClient;
import com.dejavu.backend.ai.GeminiAiClient;
import com.dejavu.backend.ai.ClaudeAiClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

public class LlmGateway {
    private final OpenAiClient openAi;
    private final GeminiAiClient gemini;
    private final ClaudeAiClient claude;
    private final ObjectMapper mapper = new ObjectMapper();
    public boolean useLowModelOnly = true;
    public String lowModelProvider = "GEMINI"; // "GEMINI", "CLAUDE", or "GPT"

    public LlmGateway(OpenAiClient openAi, GeminiAiClient gemini, ClaudeAiClient claude) {
        this.openAi = openAi;
        this.gemini = gemini;
        this.claude = claude;
    }

    public AgentAction reason(AgentState state, PerceptionFrame frame, MetacognitiveRouter router) {
        String prompt = "You are the deliberative reasoning module of an embodied agent.\n" +
            "Event: " + frame.rawEvent + "\n" +
            "Current State:\n" + state.toJson() + "\n" +
            "Metacognitive Directives: Defense Mode=" + router.defenseMode + ", Reasoning Depth=" + router.reasoningDepth + "\n\n" +
            "You MUST output ONLY a valid JSON object matching this schema:\n" +
            "{\n" +
            "  \"state_delta\": { \"stress_change\": 0.0, \"energy_change\": 0.0, \"valence\": 0.0, \"arousal\": 0.0 },\n" +
            "  \"predicted_next_event\": \"...\",\n" +
            "  \"selected_action\": { \"type\": \"reply\", \"content\": \"...\" }\n" +
            "}";
            
        String response = null;
        if (useLowModelOnly) {
            if ("CLAUDE".equalsIgnoreCase(lowModelProvider)) {
                response = claude.generateContentLight(prompt);
            } else if ("GPT".equalsIgnoreCase(lowModelProvider)) {
                response = openAi.generateContentLight(prompt);
            } else { // GEMINI default
                response = gemini.generateContentLight(prompt);
            }
            if (response != null && (response.contains("[GEMINI_ERROR]") || response.contains("[CLAUDE_ERROR]"))) response = openAi.generateContent("You are a reasoning core.", prompt);
        } else {
            response = gemini.generateContentHeavy(prompt);
            if (response == null || response.contains("ERROR")) {
                response = openAi.generateContent("You are a reasoning core.", prompt);
            }
        }
        
        try {
            if (response != null) {
                response = response.replaceAll("^```(?:json)?\\s*", "").replaceAll("```\\s*$", "").trim();
                JsonNode root = mapper.readTree(response);
                
                // Apply State Deltas
                if (root.has("state_delta")) {
                    JsonNode delta = root.get("state_delta");
                    if (delta.has("stress_change")) state.body.stress = Math.min(1.0, Math.max(0.0, state.body.stress + delta.get("stress_change").asDouble()));
                    if (delta.has("energy_change")) state.body.energy = Math.min(1.0, Math.max(0.0, state.body.energy + delta.get("energy_change").asDouble()));
                    if (delta.has("valence")) state.affect.valence = delta.get("valence").asDouble();
                    if (delta.has("arousal")) state.affect.arousal = delta.get("arousal").asDouble();
                }
                
                // Extract Action
                if (root.has("selected_action")) {
                    JsonNode actionNode = root.get("selected_action");
                    return new AgentAction(actionNode.has("type") ? actionNode.get("type").asText() : "reply", actionNode.has("content") ? actionNode.get("content").asText() : "...");
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to parse LLM JSON: " + response);
        }
        return new AgentAction("reply", "*Internal confusion* I am unable to process that properly.");
    }
}
