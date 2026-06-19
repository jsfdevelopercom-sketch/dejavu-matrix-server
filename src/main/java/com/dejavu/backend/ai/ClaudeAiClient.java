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
        if (this.apiKey != null) {
            this.apiKey = this.apiKey.replace("\"", "").replace("'", "").trim();
        }
        org.springframework.http.client.SimpleClientHttpRequestFactory factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(180000);
        this.restTemplate = new RestTemplate(factory);
    }

    public String generateContentHeavy(String prompt) {
        return doGenerate(prompt, heavyModel, false);
    }

    public String generateContentMedium(String prompt) {
        return doGenerate(prompt, mediumModel, false);
    }

    public String generateContentLight(String prompt) {
        return doGenerate(prompt, lightModel, false);
    }

    public String generateContentHeavy(String prompt, boolean isFallback) {
        return doGenerate(prompt, heavyModel, isFallback);
    }

    public String generateContentMedium(String prompt, boolean isFallback) {
        return doGenerate(prompt, mediumModel, isFallback);
    }

    public String generateContentLight(String prompt, boolean isFallback) {
        return doGenerate(prompt, lightModel, isFallback);
    }

    public String generateContent(String prompt) {
        return doGenerate(prompt, heavyModel, false);
    }

    public static boolean ALL_MODELS_ENABLED = true;
    public static boolean HIGH_MODELS_ENABLED = true;

    private String doGenerate(String prompt, String targetModel, boolean isFallback) {
        if (!ALL_MODELS_ENABLED) {
            return "[CLAUDE_ERROR] ALL_MODELS_DISABLED by Root Switch.";
        }
        if (!HIGH_MODELS_ENABLED && targetModel.equals(heavyModel)) {
            return "[CLAUDE_ERROR] HIGH_MODELS_DISABLED by Root Switch.";
        }

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
            if (!isFallback && geminiAiClient != null) {
                return targetModel.equals(lightModel) ? geminiAiClient.generateContentLight(prompt, true) : geminiAiClient.generateContentHeavy(prompt, true);
            }
            if (!isFallback && openAiClient != null && !gptDisabled) return openAiClient.generateContent(prompt);
            return "[CLAUDE_ERROR] API key missing or AI disabled.";
        }

        try {
            String url = "https://api.anthropic.com/v1/messages";

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", targetModel);
            
            if (targetModel.contains("opus")) {
                requestBody.put("max_tokens", 20000);
                Map<String, Object> thinking = new HashMap<>();
                thinking.put("type", "adaptive");
                requestBody.put("thinking", thinking);
            } else {
                requestBody.put("max_tokens", 4096);
            }

            Map<String, Object> message = new HashMap<>();
            message.put("role", "user");
            if (prompt.contains("[IMAGE_BASE64:")) {
                int start = prompt.indexOf("[IMAGE_BASE64:");
                int end = prompt.indexOf("]", start);
                if (start != -1 && end != -1) {
                    String tag = prompt.substring(start, end + 1);
                    String[] parts = tag.replace("[IMAGE_BASE64:", "").replace("]", "").split(":");
                    if (parts.length >= 2) {
                        String mimeType = parts[0];
                        String base64Data = parts[1];
                        String textPrompt = prompt.replace(tag, "").trim();

                        List<Map<String, Object>> contentList = new java.util.ArrayList<>();
                        
                        Map<String, Object> textPart = new HashMap<>();
                        textPart.put("type", "text");
                        textPart.put("text", textPrompt);
                        contentList.add(textPart);

                        Map<String, Object> imagePart = new HashMap<>();
                        imagePart.put("type", "image");
                        Map<String, Object> source = new HashMap<>();
                        source.put("type", "base64");
                        source.put("media_type", mimeType);
                        source.put("data", base64Data);
                        imagePart.put("source", source);
                        contentList.add(imagePart);

                        message.put("content", contentList);
                    } else {
                        message.put("content", prompt);
                    }
                } else {
                    message.put("content", prompt);
                }
            } else {
                message.put("content", prompt);
            }
            requestBody.put("messages", List.of(message));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-api-key", apiKey);
            headers.set("anthropic-version", "2023-06-01");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            System.out.println("\n[CLAUDE DEBUG] Attempting request to " + url);
            System.out.println("[CLAUDE DEBUG] Payload body: " + objectMapper.writeValueAsString(requestBody));

            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            Map<String, Object> responseMap = objectMapper.readValue(response.getBody(), Map.class);

            // TRACK COST
            if (responseMap.containsKey("usage")) {
                Map<String, Object> usage = (Map<String, Object>) responseMap.get("usage");
                int inTokens = usage.containsKey("input_tokens") ? ((Number) usage.get("input_tokens")).intValue() : 0;
                int outTokens = usage.containsKey("output_tokens") ? ((Number) usage.get("output_tokens")).intValue() : 0;
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
            return "[CLAUDE_ERROR] Response structure invalid or empty.";
        } catch (Exception e) {
            System.err.println("Claude API call failed (" + targetModel + "): " + e.getMessage() + ". Falling back...");
            if (!isFallback && geminiAiClient != null) {
                return targetModel.equals(lightModel) ? geminiAiClient.generateContentLight(prompt, true) : geminiAiClient.generateContentHeavy(prompt, true);
            }
            if (!isFallback && openAiClient != null && !gptDisabled) return openAiClient.generateContent(prompt);
            return "[CLAUDE_ERROR] API call failed (" + targetModel + "): " + e.getMessage();
        }
    }
}
