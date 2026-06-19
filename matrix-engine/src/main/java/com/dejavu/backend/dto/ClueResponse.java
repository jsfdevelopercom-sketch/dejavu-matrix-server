package com.dejavu.backend.dto;

public class ClueResponse {
    private String message;
    private int clueNumber;
    private int userCoins; // Added to sync client state

    public ClueResponse() {}
    public ClueResponse(String message, int clueNumber, int userCoins) {
        this.message = message;
        this.clueNumber = clueNumber;
        this.userCoins = userCoins;
    }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public int getClueNumber() { return clueNumber; }
    public void setClueNumber(int clueNumber) { this.clueNumber = clueNumber; }

    public int getUserCoins() { return userCoins; }
    public void setUserCoins(int userCoins) { this.userCoins = userCoins; }
}
