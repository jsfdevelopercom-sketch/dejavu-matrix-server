package com.dejavu.backend.ai.agent;
public class PerceptionFrame {
    public String rawEvent;
    public String source;
    public PerceptionFrame(String rawEvent, String source) {
        this.rawEvent = rawEvent; this.source = source;
    }
}
