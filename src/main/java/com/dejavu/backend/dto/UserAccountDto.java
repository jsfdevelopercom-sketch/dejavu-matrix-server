package com.dejavu.backend.dto;

import com.dejavu.backend.model.UserAccount;

public class UserAccountDto {
    private Long id;
    private String username;
    private String gameName;
    private String preferredLanguage;
    private int coins;
    private int totalScore;
    private int currentStreak;
    private int bestStreak;
    private int roomsEntered;
    private int roomsCompleted;
    private int roomsFailed;
    private int confessionsCorrectlyGuessed;
    private int cluesUsed;
    private int extraCluesBoughtWithCoins;
    private int extraCluesUnlockedByAds;
    private int adsWatched;
    private boolean onboardingCompleted;

    public UserAccountDto(UserAccount user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.gameName = user.getGameName();
        this.preferredLanguage = user.getPreferredLanguage();
        this.coins = user.getCoins();
        this.totalScore = user.getTotalScore();
        this.currentStreak = user.getCurrentStreak();
        this.bestStreak = user.getBestStreak();
        this.roomsEntered = user.getRoomsEntered();
        this.roomsCompleted = user.getRoomsCompleted();
        this.roomsFailed = user.getRoomsFailed();
        this.confessionsCorrectlyGuessed = user.getConfessionsCorrectlyGuessed();
        this.cluesUsed = user.getCluesUsed();
        this.extraCluesBoughtWithCoins = user.getExtraCluesBoughtWithCoins();
        this.extraCluesUnlockedByAds = user.getExtraCluesUnlockedByAds();
        this.adsWatched = user.getAdsWatched();
        this.onboardingCompleted = user.isOnboardingCompleted();
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getGameName() { return gameName; }
    public String getPreferredLanguage() { return preferredLanguage; }
    public int getCoins() { return coins; }
    public int getTotalScore() { return totalScore; }
    public int getCurrentStreak() { return currentStreak; }
    public int getBestStreak() { return bestStreak; }
    public int getRoomsEntered() { return roomsEntered; }
    public int getRoomsCompleted() { return roomsCompleted; }
    public int getRoomsFailed() { return roomsFailed; }
    public int getConfessionsCorrectlyGuessed() { return confessionsCorrectlyGuessed; }
    public int getCluesUsed() { return cluesUsed; }
    public int getExtraCluesBoughtWithCoins() { return extraCluesBoughtWithCoins; }
    public int getExtraCluesUnlockedByAds() { return extraCluesUnlockedByAds; }
    public int getAdsWatched() { return adsWatched; }
    public boolean isOnboardingCompleted() { return onboardingCompleted; }
}
