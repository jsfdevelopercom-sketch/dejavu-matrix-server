package com.dejavu.backend.ai;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MemoryCondenser {

    @Autowired
    private ClaudeAiClient claudeAiClient;

    @Autowired
    private GeminiAiClient geminiAiClient;

    public String condense(String ltm, String stm) {
        String rawDump = "LTM:\n" + ltm + "\n\nSTM:\n" + stm;
        if (rawDump.split("\\s+").length < 50) return rawDump; // Already short

        String instruction = "Convert the following raw memory dump into an under 200 word summary. " +
                "You MUST NOT waste words on language grammar. Be like bullet pointed very high density short phrases. " +
                "EXAMPLE BAD: on the night of 19th when the mood was tense i was angry and thought i would go and call ramon and yell at him " +
                "EXAMPLE GOOD: 19th night mood:tense, anger; action- thought to call ramon to yell. " +
                "\n\nRaw Dump:\n" + rawDump;

        // Use ONLY low model
        String condensed = geminiAiClient.generateContentLight(instruction);
        if (condensed == null) {
            condensed = claudeAiClient.generateContentLight(instruction);
        }
        
        return condensed != null ? condensed : rawDump.substring(0, Math.min(rawDump.length(), 1000));
    }
}
