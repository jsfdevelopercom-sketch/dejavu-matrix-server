package com.dejavu.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "room_sessions")
public class RoomSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private Long confessionId;
    private Long roomBlueprintId;
    
    private String state; // STARTED, WON, FAILED, EXPIRED, ABANDONED
    
    private int currentClueCount;
    private int extraCluesUsed;
    private int coinsSpentOnClues;
    private int adCluesUnlocked;
    
    private LocalDateTime timerStartedAt;
    private int timerPausedSeconds;
    private int timerDurationSeconds;
    
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getConfessionId() { return confessionId; }
    public void setConfessionId(Long confessionId) { this.confessionId = confessionId; }

    public Long getRoomBlueprintId() { return roomBlueprintId; }
    public void setRoomBlueprintId(Long roomBlueprintId) { this.roomBlueprintId = roomBlueprintId; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public int getCurrentClueCount() { return currentClueCount; }
    public void setCurrentClueCount(int currentClueCount) { this.currentClueCount = currentClueCount; }

    public int getExtraCluesUsed() { return extraCluesUsed; }
    public void setExtraCluesUsed(int extraCluesUsed) { this.extraCluesUsed = extraCluesUsed; }

    public int getCoinsSpentOnClues() { return coinsSpentOnClues; }
    public void setCoinsSpentOnClues(int coinsSpentOnClues) { this.coinsSpentOnClues = coinsSpentOnClues; }

    public int getAdCluesUnlocked() { return adCluesUnlocked; }
    public void setAdCluesUnlocked(int adCluesUnlocked) { this.adCluesUnlocked = adCluesUnlocked; }

    public LocalDateTime getTimerStartedAt() { return timerStartedAt; }
    public void setTimerStartedAt(LocalDateTime timerStartedAt) { this.timerStartedAt = timerStartedAt; }

    public int getTimerPausedSeconds() { return timerPausedSeconds; }
    public void setTimerPausedSeconds(int timerPausedSeconds) { this.timerPausedSeconds = timerPausedSeconds; }

    public int getTimerDurationSeconds() { return timerDurationSeconds; }
    public void setTimerDurationSeconds(int timerDurationSeconds) { this.timerDurationSeconds = timerDurationSeconds; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}
