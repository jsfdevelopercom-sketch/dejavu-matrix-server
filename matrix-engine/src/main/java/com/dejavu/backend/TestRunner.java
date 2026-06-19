package com.dejavu.backend;

import com.dejavu.backend.ai.ClaudeAiClient;
import com.dejavu.backend.ai.GeminiAiClient;
import com.dejavu.backend.ai.OpenAiClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class TestRunner implements CommandLineRunner {

    private final ClaudeAiClient claude;
    private final GeminiAiClient gemini;
    private final OpenAiClient openai;

    public TestRunner(ClaudeAiClient claude, GeminiAiClient gemini, OpenAiClient openai) {
        this.claude = claude;
        this.gemini = gemini;
        this.openai = openai;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("=== TESTING CLAUDE LIGHT ===");
        String cl = claude.generateContentLight("Say hello world in 1 word.");
        System.out.println("Claude Light: " + cl);

        System.out.println("=== TESTING GEMINI LIGHT ===");
        String gl = gemini.generateContentLight("Say hello world in 1 word.");
        System.out.println("Gemini Light: " + gl);

        System.out.println("=== TESTING OPENAI ===");
        String oa = openai.generateContent("Say hello world in 1 word.");
        System.out.println("OpenAI: " + oa);
    }
}
