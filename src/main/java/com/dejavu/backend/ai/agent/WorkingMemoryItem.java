package com.dejavu.backend.ai.agent;
public class WorkingMemoryItem {
    public String id;
    public String content;
    public double salience;
    public int expiresInTurns;

    public WorkingMemoryItem(String id, String content, double salience, int expiresInTurns) {
        this.id = id; this.content = content; this.salience = salience; this.expiresInTurns = expiresInTurns;
    }
}
