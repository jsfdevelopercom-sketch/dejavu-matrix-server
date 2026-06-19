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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ClaudeAiClient {

    private String apiKey;

    @Autowired
    @Lazy
    private GeminiAiClient geminiAiClient;

    @Autowired
    @Lazy
    private OpenAiClient openAiClient;

    @Autowired
    private CostTracker costTracker;

    @Autowired
    private CostLimiter costLimiter;

    @Value("${claude.model.heavy:claude-opus-4-8}")
    private String heavyModel;

    @Value("${claude.model.medium:claude-sonnet-4-6}")
    private String mediumModel;

    @Value("${claude.model.light:claude-haiku-4-5-20251001}")
    private String lightModel;

    @Value("${ai.enabled:true}")
    private boolean aiEnabled;

    @Value("${gpt.disabled:false}")
    private boolean gptDisabled;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ClaudeAiClient(@Value("${CLAUDE_API_KEY:}") String injectedKey) {
        this.apiKey = injectedKey;
        if (this.apiKey == null || this.apiKey.trim().isEmpty()) {
            this.apiKey = System.getenv("CLAUDE_API_KEY");
        }
        if (this.apiKey == null || this.apiKey.trim().isEmpty()) {
            this.apiKey = System.getenv("ANTHROPIC_API_KEY");
        }
        org.springframework.http.client.SimpleClientHttpRequestFactory factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(180000);
        this.restTemplate = new RestTemplate(factory);
    }

    public String generateContentHeavy(String prompt) {
        return doGenerate(prompt, heavyModel);
    }

    public String generateContentMedium(String prompt) {
        return doGenerate(prompt, mediumModel);
    }

    public String generateContentLight(String prompt) {
        return doGenerate(prompt, lightModel);
    }

    public String generateContent(String prompt) {
        return doGenerate(prompt, heavyModel);
    }

    private String doGenerate(String prompt, String targetModel) {
        if (costLimiter != null && costLimiter.isApiCutOff()) {
            System.err.println("API CUTOFF ENGAGED. CLAUDE CALL DROPPED.");
            return null;
        }

        if (costLimiter != null) {
            if (targetModel.equals(heavyModel)) {
                prompt = costLimiter.enforcePromptSizeLimit(prompt, "HIGH");
            } else if (targetModel.equals(mediumModel)) {
                prompt = costLimiter.enforcePromptSizeLimit(prompt, "MID");
            }
        }

        if (!aiEnabled || apiKey == null || apiKey.trim().isEmpty()) {
            System.err.println("Claude AI is disabled or API key is missing. Using Fallbacks.");
            if (geminiAiClient != null) {
                return targetModel.equals(lightModel) ? geminiAiClient.generateContentLight(prompt) : geminiAiClient.generateContentHeavy(prompt);
            }
            if (openAiClient != null && !gptDisabled) return openAiClient.generateContent(prompt);
            return null;
        }

        try {
            String url = "https://api.anthropic.com/v1/messages";

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", targetModel);
            requestBody.put("max_tokens", 4096);

            Map<String, Object> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", prompt);
            requestBody.put("messages", List.of(message));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-api-key", apiKey);
            headers.set("anthropic-version", "2023-06-01");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            Map<String, Object> responseMap = objectMapper.readValue(response.getBody(), Map.class);

            // TRACK COST
            if (responseMap.containsKey("usage")) {
                Map<String, Object> usage = (Map<String, Object>) responseMap.get("usage");
                int inTokens = (Integer) usage.getOrDefault("input_tokens", 0);
                int outTokens = (Integer) usage.getOrDefault("output_tokens", 0);
                if (costTracker != null) {
                    costTracker.trackCost("CLAUDE", targetModel, inTokens, outTokens);
                    costLimiter.checkLimits();
                }
            }

            List<Map<String, Object>> content = (List<Map<String, Object>>) responseMap.get("content");
            if (content != null && !content.isEmpty()) {
                Map<String, Object> textPart = content.get(0);
                if ("text".equals(textPart.get("type"))) {
                    return (String) textPart.get("text");
                }
            }
            return null;
        } catch (Exception e) {
            System.err.println("Claude API call failed (" + targetModel + "): " + e.getMessage() + ". Falling back...");
            if (geminiAiClient != null) {
                return targetModel.equals(lightModel) ? geminiAiClient.generateContentLight(prompt) : geminiAiClient.generateContentHeavy(prompt);
            }
            if (openAiClient != null && !gptDisabled) return openAiClient.generateContent(prompt);
            return null;
        }
    }
}
