package com.dejavu.backend.ai.agent;
public class SleepConsolidator {
    public void consolidate(AgentState state) {
        state.body.energy = 1.0;
        state.body.fatigue = 0.0;
        state.body.stress *= 0.5;
        state.memory.workingMemory.clear();
        state.selfModel.identityThreat = 0.0;
    }
}
