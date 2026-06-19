package com.dejavu.backend.dto.game;

public class PlayCardRequest {
    private String instanceId;
    private String declaredFamily; // For special cards

    public String getInstanceId() { return instanceId; }
    public void setInstanceId(String instanceId) { this.instanceId = instanceId; }

    public String getDeclaredFamily() { return declaredFamily; }
    public void setDeclaredFamily(String declaredFamily) { this.declaredFamily = declaredFamily; }
}
