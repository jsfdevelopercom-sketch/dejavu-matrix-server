package com.dejavu.backend.ai;

import org.springframework.beans.factory.annotation.Value;
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

    @Value("${gemini.model:gemini-2.5-pro}")
    private String heavyModel;
    public void setHeavyModel(String heavyModel) { this.heavyModel = heavyModel; }
    public String getHeavyModel() { return heavyModel; }
    
    @Value("${gemini.model.light:gemini-2.5-flash}")
    private String lightModel;
    public void setLightModel(String lightModel) { this.lightModel = lightModel; }
    public String getLightModel() { return lightModel; }

    @Value("${ai.enabled:true}")
    private boolean aiEnabled;
    public void setAiEnabled(boolean aiEnabled) { this.aiEnabled = aiEnabled; }
    public boolean isAiEnabled() { return aiEnabled; }

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
        if (!aiEnabled || apiKey == null || apiKey.trim().isEmpty()) {
            System.err.println("Gemini AI is disabled or API key is missing. Returning null to trigger fallback.");
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
            System.err.println("Gemini API call failed (" + targetModel + "): " + e.getMessage());
            return null;
        }
    }
}
