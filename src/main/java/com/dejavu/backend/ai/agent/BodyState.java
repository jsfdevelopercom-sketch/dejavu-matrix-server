package com.dejavu.backend.ai.agent;
public class BodyState {
    public double energy = 0.8;
    public double arousal = 0.5;
    public double stress = 0.2;
    public double fatigue = 0.2;
    public double pain = 0.0;
    public double socialHunger = 0.5;
    public double threatLevel = 0.1;
    public double curiosity = 0.5;
    public double confidence = 0.7;

    public void decay(int hours) {
        energy = Math.max(0, energy - (0.05 * hours));
        fatigue = Math.min(1.0, fatigue + (0.05 * hours));
        socialHunger = Math.min(1.0, socialHunger + (0.02 * hours));
    }
}
