package com.dejavu.backend.ai.agent;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class AgentMindTest {

    @Test
    public void testBodyStateDecay() {
        BodyState body = new BodyState();
        body.energy = 0.8;
        body.fatigue = 0.2;
        body.decay(2);
        
        assertEquals(0.7, body.energy, 0.01, "Energy should decay by 0.1 over 2 hours");
        assertEquals(0.3, body.fatigue, 0.01, "Fatigue should increase by 0.1 over 2 hours");
    }

    @Test
    public void testSalienceEngine() {
        SalienceEngine engine = new SalienceEngine();
        BodyState body = new BodyState();
        body.stress = 0.5;
        SelfModel self = new SelfModel();
        
        PerceptionFrame threatFrame = new PerceptionFrame("There is a fire in the building!", "vision");
        double salience = engine.calculateSalience(threatFrame, body, self);
        
        assertTrue(salience > 0.8, "Salience should be high for fire");
        
        PerceptionFrame neutralFrame = new PerceptionFrame("The clock is ticking.", "hearing");
        double lowSalience = engine.calculateSalience(neutralFrame, body, self);
        assertTrue(lowSalience < 0.5, "Salience should be low for neutral events");
    }

    @Test
    public void testGoalEngine() {
        GoalEngine engine = new GoalEngine();
        BodyState body = new BodyState();
        body.energy = 0.1; // Low energy
        body.socialHunger = 0.9; // High social hunger
        
        engine.evaluateGoals(body);
        
        boolean hasSleepGoal = engine.activeGoals.stream().anyMatch(g -> g.id.equals("sleep"));
        boolean hasSocialGoal = engine.activeGoals.stream().anyMatch(g -> g.id.equals("social"));
        
        assertTrue(hasSleepGoal, "Should generate sleep goal when energy is low");
        assertTrue(hasSocialGoal, "Should generate social goal when social hunger is high");
    }

    @Test
    public void testMetacognitiveRouter() {
        MetacognitiveRouter router = new MetacognitiveRouter();
        SalienceEngine engine = new SalienceEngine();
        BodyState body = new BodyState();
        SelfModel self = new SelfModel();
        
        PerceptionFrame frame = new PerceptionFrame("A normal day.", "vision");
        router.evaluate(frame, engine, body, self);
        
        assertFalse(router.llmNeeded, "LLM should not be needed for low salience mundane events");
        
        PerceptionFrame questionFrame = new PerceptionFrame("What is your name?", "speech");
        router.evaluate(questionFrame, engine, body, self);
        assertTrue(router.llmNeeded, "LLM should be needed for questions");
    }

    @Test
    public void testSleepConsolidation() {
        AgentState state = new AgentState();
        state.body.energy = 0.2;
        state.body.stress = 0.8;
        state.memory.addWorkingMemory(new WorkingMemoryItem("1", "Memory 1", 0.5, 10));
        
        SleepConsolidator sleep = new SleepConsolidator();
        sleep.consolidate(state);
        
        assertEquals(1.0, state.body.energy, 0.01, "Energy should restore to 1.0");
        assertEquals(0.4, state.body.stress, 0.01, "Stress should halve");
        assertTrue(state.memory.workingMemory.isEmpty(), "Working memory should clear on sleep");
    }
}
