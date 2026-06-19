package com.dejavu.backend.dto;

public class AdRewardCompleteRequest {
    private Long userId;
    private Long roomSessionId;
    private int clueNumber;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getRoomSessionId() { return roomSessionId; }
    public void setRoomSessionId(Long roomSessionId) { this.roomSessionId = roomSessionId; }

    public int getClueNumber() { return clueNumber; }
    public void setClueNumber(int clueNumber) { this.clueNumber = clueNumber; }
}
