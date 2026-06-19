package com.dejavu.backend;

import com.dejavu.backend.ai.OpenAiClient;

public class TestOpenAi {
    public static void main(String[] args) {
        OpenAiClient client = new OpenAiClient(System.getenv("OPENAI_API_KEY"));
        client.setGptModel("gpt-5.5"); // default
        
        System.out.println("Testing gpt-4o...");
        String r1 = client.generateContent("Say hello.", "Hello", "gpt-4o");
        System.out.println("gpt-4o: " + r1);
        
        System.out.println("Testing gpt-5.4-mini...");
        String r2 = client.generateContent("Say hello.", "Hello", "gpt-5.4-mini");
        System.out.println("gpt-5.4-mini: " + r2);
        
        System.out.println("Testing gpt-5.4-nano...");
        String r3 = client.generateContent("Say hello.", "Hello", "gpt-5.4-nano");
        System.out.println("gpt-5.4-nano: " + r3);
    }
}
