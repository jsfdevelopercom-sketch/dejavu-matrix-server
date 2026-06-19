package com.dejavu.backend.ai;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

@Component
public class GeminiAiClient {

    private String apiKey;
    
    @Autowired
    @Lazy
    private OpenAiClient openAiClient;

    @Autowired
    @Lazy
    private ClaudeAiClient claudeAiClient;

    @Autowired
    private CostTracker costTracker;

    @Autowired
    private CostLimiter costLimiter;

    @Value("${gemini.model:gemini-3.1-pro-preview}")
    private String heavyModel;
    public void setHeavyModel(String heavyModel) { this.heavyModel = heavyModel; }
    public String getHeavyModel() { return heavyModel; }
    
    @Value("${gemini.model.light:gemini-3-flash-preview}")
    private String lightModel;
    public void setLightModel(String lightModel) { this.lightModel = lightModel; }
    public String getLightModel() { return lightModel; }

    @Value("${ai.enabled:true}")
    private boolean aiEnabled;
    public void setAiEnabled(boolean aiEnabled) { this.aiEnabled = aiEnabled; }
    public boolean isAiEnabled() { return aiEnabled; }

    @Value("${gpt.disabled:false}")
    private boolean gptDisabled;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GeminiAiClient(@Value("${GEMINI_API_KEY:}") String injectedKey) {
        this.apiKey = injectedKey;
        if (this.apiKey == null || this.apiKey.trim().isEmpty()) {
            this.apiKey = System.getenv("GEMINI_API_KEY");
        }
        if (this.apiKey == null || this.apiKey.trim().isEmpty()) {
            this.apiKey = System.getenv("GEMINI_KEY");
        }
        org.springframework.http.client.SimpleClientHttpRequestFactory factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000); // 10 seconds
        factory.setReadTimeout(180000); // 3 minutes maximum for heavy models
        this.restTemplate = new RestTemplate(factory);
    }

    public String generateContentHeavy(String prompt) {
        return doGenerate(prompt, heavyModel);
    }
    
    public String generateContentLight(String prompt) {
        return doGenerate(prompt, lightModel);
    }
    
    // Kept for backwards compatibility just in case, but routes to heavy by default
    public String generateContent(String prompt) {
        return doGenerate(prompt, heavyModel);
    }

    private String doGenerate(String prompt, String targetModel) {
        if (costLimiter != null && costLimiter.isApiCutOff()) {
            System.err.println("API CUTOFF ENGAGED. GEMINI CALL DROPPED.");
            return null;
        }

        if (costLimiter != null && targetModel.equals(heavyModel)) {
            prompt = costLimiter.enforcePromptSizeLimit(prompt, "HIGH");
        }

        if (!aiEnabled || apiKey == null || apiKey.trim().isEmpty()) {
            System.err.println("Gemini AI is disabled or API key is missing. Using Fallbacks.");
            if (claudeAiClient != null) {
                 return targetModel.equals(lightModel) ? claudeAiClient.generateContentLight(prompt) : claudeAiClient.generateContentHeavy(prompt);
            }
            if (openAiClient != null && !gptDisabled) return openAiClient.generateContent(prompt);
            return null;
        }

        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/" + targetModel + ":generateContent?key=" + apiKey;
            
            Map<String, Object> requestBody = new HashMap<>();
            Map<String, Object> content = new HashMap<>();
            Map<String, Object> part = new HashMap<>();
            
            part.put("text", prompt);
            content.put("parts", List.of(part));
            requestBody.put("contents", List.of(content));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            
            // Basic extraction (Assuming standard Gemini JSON response structure)
            Map<String, Object> responseMap = objectMapper.readValue(response.getBody(), Map.class);

            // TRACK COST
            if (responseMap.containsKey("usageMetadata")) {
                Map<String, Object> usage = (Map<String, Object>) responseMap.get("usageMetadata");
                int inTokens = (Integer) usage.getOrDefault("promptTokenCount", 0);
                int outTokens = (Integer) usage.getOrDefault("candidatesTokenCount", 0);
                if (costTracker != null) {
                    costTracker.trackCost("GEMINI", targetModel, inTokens, outTokens);
                    costLimiter.checkLimits();
                }
            }

            List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseMap.get("candidates");
            if (candidates != null && !candidates.isEmpty()) {
                Map<String, Object> candidate = candidates.get(0);
                Map<String, Object> resContent = (Map<String, Object>) candidate.get("content");
                if (resContent != null) {
                    List<Map<String, Object>> parts = (List<Map<String, Object>>) resContent.get("parts");
                    if (parts != null && !parts.isEmpty()) {
                        return (String) parts.get(0).get("text");
                    }
                }
            }
            return null;
        } catch (Exception e) {
            System.err.println("Gemini API call failed (" + targetModel + "): " + e.getMessage() + ". Falling back...");
            if (claudeAiClient != null) {
                 return targetModel.equals(lightModel) ? claudeAiClient.generateContentLight(prompt) : claudeAiClient.generateContentHeavy(prompt);
            }
            if (openAiClient != null && !gptDisabled) return openAiClient.generateContent(prompt);
            return null;
        }
    }
}
