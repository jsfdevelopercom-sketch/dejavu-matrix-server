package com.dejavu.backend.ai.agent;
public class MetacognitiveRouter {
    public boolean llmNeeded;
    public String reasoningDepth;
    public String defenseMode;
    public double uncertainty;

    public void evaluate(PerceptionFrame frame, SalienceEngine salienceEngine, BodyState bodyState, SelfModel selfModel) {
        double salience = salienceEngine.calculateSalience(frame, bodyState, selfModel);
        if (salience > 0.8 || frame.rawEvent.contains("?")) {
            llmNeeded = true;
            reasoningDepth = "deep";
        } else {
            llmNeeded = false;
            reasoningDepth = "shallow";
        }
        defenseMode = selfModel.identityThreat > 0.7 ? "rationalization" : "none";
        uncertainty = 1.0 - bodyState.confidence;
    }
}
