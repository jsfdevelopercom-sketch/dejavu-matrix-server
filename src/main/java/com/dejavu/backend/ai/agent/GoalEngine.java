package com.dejavu.backend.ai.agent;
import java.util.List;
import java.util.ArrayList;
public class GoalEngine {
    public List<Goal> activeGoals = new ArrayList<>();
    public void evaluateGoals(BodyState body) {
        if (body.energy < 0.2) activeGoals.add(new Goal("sleep", "Rest and recover energy", 0.9, "body"));
        if (body.socialHunger > 0.8) activeGoals.add(new Goal("social", "Talk to someone", 0.7, "body"));
    }
}
