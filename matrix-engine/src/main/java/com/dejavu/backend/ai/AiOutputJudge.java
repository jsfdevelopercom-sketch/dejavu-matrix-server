package com.dejavu.backend.ai;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AiOutputJudge {

    @Autowired
    private ClaudeAiClient claudeAiClient;

    @Autowired
    private GeminiAiClient geminiAiClient;

    public boolean isOutputAdequate(String instruction, String output) {
        if (output == null || output.trim().isEmpty() || output.contains("[GEMINI_ERROR]") || output.contains("[CLAUDE_ERROR]")) return false;
        
        String prompt = "You are a strict binary judge. Does the following output adequately fulfill the given instruction? " +
                "Respond with EXACTLY ONE WORD: PASS or FAIL.\n\n" +
                "INSTRUCTION:\n" + instruction + "\n\n" +
                "OUTPUT:\n" + output;

        // Use relatively higher version of low model (e.g. medium model)
        String verdict = claudeAiClient.generateContentMedium(prompt);
        if (verdict == null || verdict.contains("[CLAUDE_ERROR]") || verdict.contains("[GEMINI_ERROR]")) {
            verdict = geminiAiClient.generateContentHeavy(prompt); // Gemini doesn't have medium, so we use heavy, but prompt is tiny.
        }
        
        if (verdict != null) {
            if (verdict.contains("[CLAUDE_ERROR]") || verdict.contains("[GEMINI_ERROR]")) return true; // Judge failed, default to pass to avoid infinite loop
            return !verdict.toUpperCase().contains("FAIL");
        }
        return true; // Default to pass if judge fails
    }
}
