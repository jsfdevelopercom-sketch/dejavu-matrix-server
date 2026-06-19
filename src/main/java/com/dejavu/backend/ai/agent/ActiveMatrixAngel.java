package com.dejavu.backend.ai.agent;

import com.dejavu.backend.ai.ClaudeAiClient;
import com.dejavu.backend.ai.DarkArchangelInterviewEngine;
import com.dejavu.backend.ai.GeminiAiClient;
import com.dejavu.backend.ai.MemoryCondenser;
import com.dejavu.backend.ai.AiOutputJudge;
import com.dejavu.backend.ai.OpenAiClient;
import com.dejavu.backend.ai.PersonalityEngine;
import com.dejavu.backend.ai.RelationsEngine;
import com.dejavu.backend.model.Confession;
import com.dejavu.backend.model.MatrixAngel;
import com.dejavu.backend.model.game.ConfessionGameContent;

public class ActiveMatrixAngel extends ActiveMatrixAgent {

    private final DarkArchangelInterviewEngine archangelEngine;
    private final MatrixAngel angelEntity;

    public ActiveMatrixAngel(MatrixAngel entity, 
                             OpenAiClient openAiClient, 
                             GeminiAiClient geminiAiClient, 
                             ClaudeAiClient claudeAiClient, 
                             MemoryCondenser memoryCondenser, 
                             AiOutputJudge outputJudge, 
                             PersonalityEngine personalityEngine, 
                             RelationsEngine relationsEngine,
                             DarkArchangelInterviewEngine archangelEngine) {
        super(entity, openAiClient, geminiAiClient, claudeAiClient, memoryCondenser, outputJudge, personalityEngine, relationsEngine);
        this.angelEntity = entity;
        this.archangelEngine = archangelEngine;
    }

    /**
     * The Angel uses the Archangel Engine as a tool to extract and judge human confessions.
     */
    public ConfessionGameContent passJudgment(Confession confession) {
        String thoughts = ponder("I am observing a new confession: " + confession.getText() + ". It is time to pass judgment.");
        logEvent("[DIVINE JUDGMENT] " + thoughts);
        
        // Use the engine as a tool
        ConfessionGameContent content = archangelEngine.generateGameContent(confession);
        
        logEvent("[JUDGMENT COMPLETE] Successfully extracted fragments and sealed the fate of the confessor.");
        return content;
    }

    public String exertDomainInfluence() {
        return ponder("I am exerting my influence over the domain of " + angelEntity.getDomain() + ". The mortals shall feel my presence.");
    }
}
