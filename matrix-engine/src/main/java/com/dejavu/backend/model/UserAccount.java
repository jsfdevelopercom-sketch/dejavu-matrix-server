package com.dejavu.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_accounts")
public class UserAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    private String gameName;

    private String preferredLanguage = "English";

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
    private int purchasedCoins;
    
    private boolean onboardingCompleted;

    private LocalDateTime createdAt;
    private LocalDateTime lastActiveAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        lastActiveAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        lastActiveAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getGameName() { return gameName; }
    public void setGameName(String gameName) { this.gameName = gameName; }

    public String getPreferredLanguage() { return preferredLanguage; }
    public void setPreferredLanguage(String preferredLanguage) { this.preferredLanguage = preferredLanguage; }

    public int getCoins() { return coins; }
    public void setCoins(int coins) { this.coins = coins; }

    public int getTotalScore() { return totalScore; }
    public void setTotalScore(int totalScore) { this.totalScore = totalScore; }

    public int getCurrentStreak() { return currentStreak; }
    public void setCurrentStreak(int currentStreak) { this.currentStreak = currentStreak; }

    public int getBestStreak() { return bestStreak; }
    public void setBestStreak(int bestStreak) { this.bestStreak = bestStreak; }

    public int getRoomsEntered() { return roomsEntered; }
    public void setRoomsEntered(int roomsEntered) { this.roomsEntered = roomsEntered; }

    public int getRoomsCompleted() { return roomsCompleted; }
    public void setRoomsCompleted(int roomsCompleted) { this.roomsCompleted = roomsCompleted; }

    public int getRoomsFailed() { return roomsFailed; }
    public void setRoomsFailed(int roomsFailed) { this.roomsFailed = roomsFailed; }

    public int getConfessionsCorrectlyGuessed() { return confessionsCorrectlyGuessed; }
    public void setConfessionsCorrectlyGuessed(int confessionsCorrectlyGuessed) { this.confessionsCorrectlyGuessed = confessionsCorrectlyGuessed; }

    public int getCluesUsed() { return cluesUsed; }
    public void setCluesUsed(int cluesUsed) { this.cluesUsed = cluesUsed; }

    public int getExtraCluesBoughtWithCoins() { return extraCluesBoughtWithCoins; }
    public void setExtraCluesBoughtWithCoins(int extraCluesBoughtWithCoins) { this.extraCluesBoughtWithCoins = extraCluesBoughtWithCoins; }

    public int getExtraCluesUnlockedByAds() { return extraCluesUnlockedByAds; }
    public void setExtraCluesUnlockedByAds(int extraCluesUnlockedByAds) { this.extraCluesUnlockedByAds = extraCluesUnlockedByAds; }

    public int getAdsWatched() { return adsWatched; }
    public void setAdsWatched(int adsWatched) { this.adsWatched = adsWatched; }

    public int getPurchasedCoins() { return purchasedCoins; }
    public void setPurchasedCoins(int purchasedCoins) { this.purchasedCoins = purchasedCoins; }

    public boolean isOnboardingCompleted() { return onboardingCompleted; }
    public void setOnboardingCompleted(boolean onboardingCompleted) { this.onboardingCompleted = onboardingCompleted; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getLastActiveAt() { return lastActiveAt; }
    public void setLastActiveAt(LocalDateTime lastActiveAt) { this.lastActiveAt = lastActiveAt; }
}
