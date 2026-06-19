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

    public GeminiAiClient(@Value("${gemini.api.key:}") String injectedKey) {
        this.apiKey = injectedKey;
        if (this.apiKey == null || this.apiKey.trim().isEmpty()) {
            this.apiKey = System.getenv("GEMINI_API_KEY");
        }
        if (this.apiKey != null) {
            this.apiKey = this.apiKey.replace("\"", "").replace("'", "").trim();
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
        return doGenerate(prompt, heavyModel, false);
    }
    
    public String generateContentLight(String prompt) {
        return doGenerate(prompt, lightModel, false);
    }

    public String generateContentHeavy(String prompt, boolean isFallback) {
        return doGenerate(prompt, heavyModel, isFallback);
    }
    
    public String generateContentLight(String prompt, boolean isFallback) {
        return doGenerate(prompt, lightModel, isFallback);
    }
    
    // Kept for backwards compatibility just in case, but routes to heavy by default
    public String generateContent(String prompt) {
        return doGenerate(prompt, heavyModel, false);
    }

    public static boolean ALL_MODELS_ENABLED = true;
    public static boolean HIGH_MODELS_ENABLED = true;

    private String doGenerate(String prompt, String targetModel, boolean isFallback) {
        if (!ALL_MODELS_ENABLED) {
            return "[GEMINI_ERROR] ALL_MODELS_DISABLED by Root Switch.";
        }
        if (!HIGH_MODELS_ENABLED && targetModel.equals(heavyModel)) {
            return "[GEMINI_ERROR] HIGH_MODELS_DISABLED by Root Switch.";
        }

        if (costLimiter != null && costLimiter.isApiCutOff()) {
            System.err.println("API CUTOFF ENGAGED. GEMINI CALL DROPPED.");
            return null;
        }

        if (costLimiter != null && targetModel.equals(heavyModel)) {
            prompt = costLimiter.enforcePromptSizeLimit(prompt, "HIGH");
        }

        if (!aiEnabled || apiKey == null || apiKey.trim().isEmpty()) {
            System.err.println("Gemini AI is disabled or API key is missing. Using Fallbacks.");
            if (!isFallback && claudeAiClient != null) {
                 return targetModel.equals(lightModel) ? claudeAiClient.generateContentLight(prompt, true) : claudeAiClient.generateContentHeavy(prompt, true);
            }
            if (!isFallback && openAiClient != null && !gptDisabled) return openAiClient.generateContent(prompt);
            return "[GEMINI_ERROR] API key missing or AI disabled.";
        }

        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/" + targetModel + ":generateContent?key=" + apiKey;
            
            Map<String, Object> requestBody = new HashMap<>();
            Map<String, Object> content = new HashMap<>();
            List<Map<String, Object>> parts = new java.util.ArrayList<>();
            if (prompt.contains("[IMAGE_BASE64:")) {
                int start = prompt.indexOf("[IMAGE_BASE64:");
                int end = prompt.indexOf("]", start);
                if (start != -1 && end != -1) {
                    String tag = prompt.substring(start, end + 1);
                    String[] tagParts = tag.replace("[IMAGE_BASE64:", "").replace("]", "").split(":");
                    if (tagParts.length >= 2) {
                        String mimeType = tagParts[0];
                        String base64Data = tagParts[1];
                        String textPrompt = prompt.replace(tag, "").trim();

                        Map<String, Object> textPart = new HashMap<>();
                        textPart.put("text", textPrompt);
                        parts.add(textPart);

                        Map<String, Object> imagePart = new HashMap<>();
                        Map<String, String> inlineData = new HashMap<>();
                        inlineData.put("mimeType", mimeType);
                        inlineData.put("data", base64Data);
                        imagePart.put("inlineData", inlineData);
                        parts.add(imagePart);
                    } else {
                        Map<String, Object> textPart = new HashMap<>();
                        textPart.put("text", prompt);
                        parts.add(textPart);
                    }
                } else {
                    Map<String, Object> textPart = new HashMap<>();
                    textPart.put("text", prompt);
                    parts.add(textPart);
                }
            } else {
                Map<String, Object> textPart = new HashMap<>();
                textPart.put("text", prompt);
                parts.add(textPart);
            }
            content.put("parts", parts);
            requestBody.put("contents", List.of(content));

            // Disable all safety settings to prevent Empty Node blocking as per MiniAgent
            List<Map<String, String>> safetySettings = new java.util.ArrayList<>();
            safetySettings.add(Map.of("category", "HARM_CATEGORY_HARASSMENT", "threshold", "BLOCK_NONE"));
            safetySettings.add(Map.of("category", "HARM_CATEGORY_HATE_SPEECH", "threshold", "BLOCK_NONE"));
            safetySettings.add(Map.of("category", "HARM_CATEGORY_SEXUALLY_EXPLICIT", "threshold", "BLOCK_NONE"));
            safetySettings.add(Map.of("category", "HARM_CATEGORY_DANGEROUS_CONTENT", "threshold", "BLOCK_NONE"));
            requestBody.put("safetySettings", safetySettings);

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
                    List<Map<String, Object>> resParts = (List<Map<String, Object>>) resContent.get("parts");
                    if (resParts != null && !resParts.isEmpty()) {
                        return (String) resParts.get(0).get("text");
                    }
                }
            }
            return "[GEMINI_ERROR] Response structure invalid or empty.";
        } catch (Exception e) {
            System.err.println("Gemini API call failed (" + targetModel + "): " + e.getMessage() + ". Falling back...");
            if (!isFallback && claudeAiClient != null) {
                 return targetModel.equals(lightModel) ? claudeAiClient.generateContentLight(prompt, true) : claudeAiClient.generateContentHeavy(prompt, true);
            }
            if (!isFallback && openAiClient != null && !gptDisabled) return openAiClient.generateContent(prompt);
            return "[GEMINI_ERROR] API call failed (" + targetModel + "): " + e.getMessage();
        }
    }
}
