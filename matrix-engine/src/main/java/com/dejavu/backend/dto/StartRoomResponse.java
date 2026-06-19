package com.dejavu.backend.dto;

import java.util.List;

public class StartRoomResponse {
    private Long roomSessionId;
    private String roomTitle;
    private String backgroundAssetId;
    private List<String> objectAssetIds;
    private String openingAngelLine;
    private int timerDurationSeconds;
    private int coins;

    public Long getRoomSessionId() { return roomSessionId; }
    public void setRoomSessionId(Long roomSessionId) { this.roomSessionId = roomSessionId; }

    public String getRoomTitle() { return roomTitle; }
    public void setRoomTitle(String roomTitle) { this.roomTitle = roomTitle; }

    public String getBackgroundAssetId() { return backgroundAssetId; }
    public void setBackgroundAssetId(String backgroundAssetId) { this.backgroundAssetId = backgroundAssetId; }

    public List<String> getObjectAssetIds() { return objectAssetIds; }
    public void setObjectAssetIds(List<String> objectAssetIds) { this.objectAssetIds = objectAssetIds; }

    public String getOpeningAngelLine() { return openingAngelLine; }
    public void setOpeningAngelLine(String openingAngelLine) { this.openingAngelLine = openingAngelLine; }

    public int getTimerDurationSeconds() { return timerDurationSeconds; }
    public void setTimerDurationSeconds(int timerDurationSeconds) { this.timerDurationSeconds = timerDurationSeconds; }

    public int getCoins() { return coins; }
    public void setCoins(int coins) { this.coins = coins; }
}
