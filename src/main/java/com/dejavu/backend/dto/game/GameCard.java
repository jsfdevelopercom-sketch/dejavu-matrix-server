package com.dejavu.backend.dto.game;

import java.util.UUID;

public class GameCard {
    private String instanceId;
    private String type; // "STANDARD", "SPECIAL"
    private Long fragmentId; // Optional if it's a standard fragment
    private String emotionFamily;
    private Integer intensity;
    private String fragmentText;
    private String specialType; // e.g., "SILENCE", "DENIAL"

    public GameCard() {
        this.instanceId = UUID.randomUUID().toString();
    }

    public String getInstanceId() { return instanceId; }
    public void setInstanceId(String instanceId) { this.instanceId = instanceId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Long getFragmentId() { return fragmentId; }
    public void setFragmentId(Long fragmentId) { this.fragmentId = fragmentId; }

    public String getEmotionFamily() { return emotionFamily; }
    public void setEmotionFamily(String emotionFamily) { this.emotionFamily = emotionFamily; }

    public Integer getIntensity() { return intensity; }
    public void setIntensity(Integer intensity) { this.intensity = intensity; }

    public String getFragmentText() { return fragmentText; }
    public void setFragmentText(String fragmentText) { this.fragmentText = fragmentText; }

    public String getSpecialType() { return specialType; }
    public void setSpecialType(String specialType) { this.specialType = specialType; }
}
