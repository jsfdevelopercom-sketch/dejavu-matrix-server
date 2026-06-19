package com.dejavu.backend.dto;

public class GuessResponse {
    private int accuracy;
    private String tier;
    private boolean won;
    private String angelFeedback;
    private int coinsAwarded;
    private UserAccountDto updatedUserStats;
    private String actualConfession;

    public int getAccuracy() { return accuracy; }
    public void setAccuracy(int accuracy) { this.accuracy = accuracy; }

    public String getTier() { return tier; }
    public void setTier(String tier) { this.tier = tier; }

    public boolean isWon() { return won; }
    public void setWon(boolean won) { this.won = won; }

    public String getAngelFeedback() { return angelFeedback; }
    public void setAngelFeedback(String angelFeedback) { this.angelFeedback = angelFeedback; }

    public int getCoinsAwarded() { return coinsAwarded; }
    public void setCoinsAwarded(int coinsAwarded) { this.coinsAwarded = coinsAwarded; }

    public UserAccountDto getUpdatedUserStats() { return updatedUserStats; }
    public void setUpdatedUserStats(UserAccountDto updatedUserStats) { this.updatedUserStats = updatedUserStats; }

    public String getActualConfession() { return actualConfession; }
    public void setActualConfession(String actualConfession) { this.actualConfession = actualConfession; }
}
