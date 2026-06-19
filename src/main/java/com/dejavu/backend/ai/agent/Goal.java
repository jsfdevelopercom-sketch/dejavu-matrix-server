package com.dejavu.backend.ai.agent;
public class Goal {
    public String id;
    public String description;
    public double priority;
    public String origin;
    
    public Goal(String id, String description, double priority, String origin) {
        this.id = id; this.description = description; this.priority = priority; this.origin = origin;
    }
}
