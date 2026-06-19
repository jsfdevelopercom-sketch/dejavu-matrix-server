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
public class OpenAiClient {

    private String apiKey;

    @Value("${GPT_MODEL:gpt-4o}")
    private String gptModel;
    
    public void setGptModel(String gptModel) { this.gptModel = gptModel; }
    public String getGptModel() { return this.gptModel; }

    @Value("${gpt.disabled:false}")
    private boolean gptDisabled;

    @Autowired
    @Lazy
    private GeminiAiClient geminiAiClient;

    @Autowired
    private CostTracker costTracker;

    @Autowired
    private CostLimiter costLimiter;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OpenAiClient(@Value("${OPENAI_API_KEY:${GPT_API_KEY:}}") String injectedKey) {
        this.apiKey = injectedKey;
        if (this.apiKey == null || this.apiKey.trim().isEmpty()) {
            this.apiKey = System.getenv("GPT_API_KEY");
        }
        if (this.apiKey == null || this.apiKey.trim().isEmpty()) {
            this.apiKey = System.getenv("OPENAI_API_KEY");
        }
        if (this.apiKey == null || this.apiKey.trim().isEmpty()) {
            this.apiKey = System.getenv("API_KEY"); // Final desperate fallback
        }
        org.springframework.http.client.SimpleClientHttpRequestFactory factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000); // 10 seconds
        factory.setReadTimeout(180000); // 3 minutes maximum
        this.restTemplate = new RestTemplate(factory);
    }

    public String generateContent(String prompt) {
        return generateContent(null, prompt);
    }

    public String generateContent(String systemPrompt, String userPrompt) {
        return generateContent(systemPrompt, userPrompt, gptModel);
    }

    public static boolean ALL_MODELS_ENABLED = true;
    public static boolean HIGH_MODELS_ENABLED = true;

    public String generateContent(String systemPrompt, String userPrompt, String overrideModel) {
        if (!ALL_MODELS_ENABLED) {
            return "[OPENAI_ERROR] ALL_MODELS_DISABLED by Root Switch.";
        }
        // Assuming OpenAI is always considered a HIGH model in this context since it's used for heavy lifting
        if (!HIGH_MODELS_ENABLED) {
            return "[OPENAI_ERROR] HIGH_MODELS_DISABLED by Root Switch.";
        }

        if (costLimiter != null && costLimiter.isApiCutOff()) {
            System.err.println("API CUTOFF ENGAGED. OPENAI CALL DROPPED.");
            return null;
        }

        String combinedPrompt = "";
        if (systemPrompt != null && !systemPrompt.trim().isEmpty()) {
            combinedPrompt += systemPrompt + "\n\n";
        }
        if (userPrompt != null) {
            combinedPrompt += userPrompt;
        }

        if (costLimiter != null) {
            combinedPrompt = costLimiter.enforcePromptSizeLimit(combinedPrompt, "HIGH");
        }

        if (gptDisabled) {
            return geminiAiClient.generateContentHeavy(combinedPrompt);
        }

        if (apiKey == null || apiKey.trim().isEmpty()) {
            System.err.println("GPT API key is missing. Returning null.");
            return null;
        }

        try {
            String url = "https://api.openai.com/v1/chat/completions";
            String targetModel = (overrideModel != null && !overrideModel.isEmpty() && !overrideModel.contains("gpt-5")) ? overrideModel : gptModel;
            
            // Hard block on gpt-5
            if (targetModel.contains("gpt-5")) targetModel = "gpt-4o";

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", targetModel);
            
            if (systemPrompt != null && systemPrompt.contains("JSON")) {
                Map<String, String> responseFormat = new HashMap<>();
                responseFormat.put("type", "json_object");
                requestBody.put("response_format", responseFormat);
            }
            
            List<Map<String, String>> messages = new java.util.ArrayList<>();
            Map<String, String> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", combinedPrompt);
            messages.add(userMsg);
            
            requestBody.put("messages", messages);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            
            Map<String, Object> responseMap = objectMapper.readValue(response.getBody(), Map.class);
            
            // TRACK COST
            if (responseMap.containsKey("usage")) {
                Map<String, Object> usage = (Map<String, Object>) responseMap.get("usage");
                int inTokens = (Integer) usage.getOrDefault("prompt_tokens", 0);
                int outTokens = (Integer) usage.getOrDefault("completion_tokens", 0);
                if (costTracker != null) {
                    costTracker.trackCost("OPENAI", targetModel, inTokens, outTokens);
                    costLimiter.checkLimits();
                }
            }

            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> choice = choices.get(0);
                Map<String, Object> resMessage = (Map<String, Object>) choice.get("message");
                if (resMessage != null) {
                    return (String) resMessage.get("content");
                }
            }
            return null;
        } catch (Exception e) {
            System.err.println("GPT API call failed (" + gptModel + "): " + e.getMessage());
            return null;
        }
    }

    public String generateImage(String prompt) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return null;
        }
        try {
            String url = "https://api.openai.com/v1/images/generations";
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "dall-e-2");
            requestBody.put("prompt", prompt);
            requestBody.put("n", 1);
            requestBody.put("size", "256x256");
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            
            Map<String, Object> responseMap = objectMapper.readValue(response.getBody(), Map.class);
            List<Map<String, Object>> data = (List<Map<String, Object>>) responseMap.get("data");
            if (data != null && !data.isEmpty()) {
                return (String) data.get(0).get("url");
            }
        } catch (Exception e) {
            System.err.println("DALL-E Image generation failed: " + e.getMessage());
        }
        return null;
    }
}
