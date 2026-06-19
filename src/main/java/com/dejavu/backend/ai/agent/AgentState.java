package com.dejavu.backend.ai.agent;
import com.fasterxml.jackson.databind.ObjectMapper;
public class AgentState {
    public BodyState body = new BodyState();
    public AffectState affect = new AffectState();
    public MemoryStore memory = new MemoryStore();
    public SelfModel selfModel = new SelfModel();
    public SocialModel socialModel = new SocialModel();
    public GoalEngine goalEngine = new GoalEngine();
    
    public String toJson() {
        try {
            return new ObjectMapper().writeValueAsString(this);
        } catch (Exception e) {
            return "{}";
        }
    }
}
