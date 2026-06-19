package com.dejavu.backend.ai;

import com.dejavu.backend.model.RoomBlueprint;
import org.springframework.stereotype.Component;

@Component
public class FallbackRoomBlueprintGenerator {

    public RoomBlueprint generateFallback(Long confessionId, String text) {
        RoomBlueprint bp = new RoomBlueprint();
        bp.setConfessionId(confessionId);
        bp.setRoomTitle("The Forgotten Room");
        bp.setDetectedPlace("unknown");
        bp.setPrimaryEmotion("fear");
        bp.setThemesJson("[\"secrets\"]");
        bp.setBackgroundAssetId("room_bg_01");
        bp.setObjectAssetIdsJson("[\"obj_broken_mirror_01\", \"obj_candle_01\", \"obj_rusty_key_01\"]");
        bp.setOpeningAngelLine("Are you ready to touch what this room buried?");
        bp.setClue1("Something was hidden here.");
        bp.setClue2("The truth is often painful.");
        bp.setExtraClue3("A secret kept in the dark.");
        bp.setExtraClue4("What you seek is a broken bond.");
        bp.setHiddenTargetSummary(text != null ? text : "A secret confession.");
        bp.setWinAngelLine("The sand opens. But it remembers your answer.");
        bp.setGeneratedByModel(false);
        return bp;
    }
}
