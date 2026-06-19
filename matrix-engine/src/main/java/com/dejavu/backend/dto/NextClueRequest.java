package com.dejavu.backend.dto;

public class NextClueRequest {
    private String method; // FREE, COINS, AD
    private String adRewardToken;

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public String getAdRewardToken() { return adRewardToken; }
    public void setAdRewardToken(String adRewardToken) { this.adRewardToken = adRewardToken; }
}
