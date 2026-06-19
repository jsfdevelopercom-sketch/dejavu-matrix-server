package com.dejavu.backend.ai.agent;
public class SalienceEngine {
    public double calculateSalience(PerceptionFrame frame, BodyState body, SelfModel self) {
        double threatWeight = frame.rawEvent.toLowerCase().contains("fire") || frame.rawEvent.toLowerCase().contains("danger") ? 0.8 : 0.1;
        double socialWeight = frame.rawEvent.toLowerCase().contains("says") || frame.rawEvent.toLowerCase().contains("calls") ? 0.5 : 0.1;
        double salience = threatWeight + socialWeight + (body.stress * 0.2);
        return Math.min(1.0, salience);
    }
}
