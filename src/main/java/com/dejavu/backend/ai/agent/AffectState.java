package com.dejavu.backend.ai.agent;
public class AffectState {
    public double valence = 0.0; // -1 to 1
    public double arousal = 0.5; // 0 to 1
    public double dominance = 0.5; // 0 to 1
    public String dominantEmotion = "neutral";
}
