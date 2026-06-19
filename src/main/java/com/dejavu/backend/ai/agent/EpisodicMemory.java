package com.dejavu.backend.ai.agent;
public class EpisodicMemory {
    public String memoryId;
    public String summary;
    public String emotionalTag;
    public double relevance;
    public EpisodicMemory(String memoryId, String summary, String emotionalTag, double relevance) {
        this.memoryId = memoryId; this.summary = summary; this.emotionalTag = emotionalTag; this.relevance = relevance;
    }
}
