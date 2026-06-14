package com.dejavu.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ad_rewards")
public class AdReward {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private Long roomSessionId;
    private int clueNumberUnlocked;
    private String adProvider;
    private boolean adCompleted;
    
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getRoomSessionId() { return roomSessionId; }
    public void setRoomSessionId(Long roomSessionId) { this.roomSessionId = roomSessionId; }

    public int getClueNumberUnlocked() { return clueNumberUnlocked; }
    public void setClueNumberUnlocked(int clueNumberUnlocked) { this.clueNumberUnlocked = clueNumberUnlocked; }

    public String getAdProvider() { return adProvider; }
    public void setAdProvider(String adProvider) { this.adProvider = adProvider; }

    public boolean isAdCompleted() { return adCompleted; }
    public void setAdCompleted(boolean adCompleted) { this.adCompleted = adCompleted; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
