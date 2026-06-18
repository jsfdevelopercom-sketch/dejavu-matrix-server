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
public class OpenAiClient {

    private String apiKey;

    @Value("${GPT_MODEL:gpt-5.5}")
    private String gptModel;
    
    public void setGptModel(String gptModel) { this.gptModel = gptModel; }
    public String getGptModel() { return this.gptModel; }

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

    public String generateContent(String systemPrompt, String userPrompt, String overrideModel) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            System.err.println("GPT API key is missing. Returning null.");
            return null;
        }

        try {
            String url = "https://api.openai.com/v1/chat/completions";
            
            String targetModel = (overrideModel != null && !overrideModel.isEmpty()) ? overrideModel : gptModel;
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", targetModel);
            
            List<Map<String, String>> messages = new java.util.ArrayList<>();
            if (systemPrompt != null && !systemPrompt.trim().isEmpty()) {
                Map<String, String> sysMsg = new HashMap<>();
                sysMsg.put("role", "system");
                sysMsg.put("content", systemPrompt);
                messages.add(sysMsg);
            }
            
            Map<String, String> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", userPrompt);
            messages.add(userMsg);
            
            requestBody.put("messages", messages);
            

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            
            Map<String, Object> responseMap = objectMapper.readValue(response.getBody(), Map.class);
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
            requestBody.put("model", "dall-e-3");
            requestBody.put("prompt", prompt);
            requestBody.put("n", 1);
            requestBody.put("size", "1024x1024");
            
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
