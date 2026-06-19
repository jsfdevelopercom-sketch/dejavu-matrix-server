package com.dejavu.backend.model.game;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "game_sessions")
public class GameSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String status; // "ACTIVE", "FINISHED"
    private Long humanPlayerId;
    private Long aiOpponentId; // Optional for VS AI
    private String turnPlayer; // "PLAYER" or "AI"
    
    @Column(columnDefinition = "TEXT")
    private String playerHand; // JSON array of card IDs/Data
    
    @Column(columnDefinition = "TEXT")
    private String aiHand;
    
    @Column(columnDefinition = "TEXT")
    private String drawPile;
    
    @Column(columnDefinition = "TEXT")
    private String discardPile;
    
    @Column(columnDefinition = "TEXT")
    private String storyStripFragmentIds;

    private String currentFamily;
    private Integer currentIntensity;
    private String pendingEffects;
    private Long winnerId;
    
    private Long activeJudgmentId; // If someone needs to answer a judgment
    private String activeJudgmentTarget; // "PLAYER" or "AI"

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Long getHumanPlayerId() { return humanPlayerId; }
    public void setHumanPlayerId(Long humanPlayerId) { this.humanPlayerId = humanPlayerId; }

    public Long getAiOpponentId() { return aiOpponentId; }
    public void setAiOpponentId(Long aiOpponentId) { this.aiOpponentId = aiOpponentId; }

    public String getTurnPlayer() { return turnPlayer; }
    public void setTurnPlayer(String turnPlayer) { this.turnPlayer = turnPlayer; }

    public String getPlayerHand() { return playerHand; }
    public void setPlayerHand(String playerHand) { this.playerHand = playerHand; }

    public String getAiHand() { return aiHand; }
    public void setAiHand(String aiHand) { this.aiHand = aiHand; }

    public String getDrawPile() { return drawPile; }
    public void setDrawPile(String drawPile) { this.drawPile = drawPile; }

    public String getDiscardPile() { return discardPile; }
    public void setDiscardPile(String discardPile) { this.discardPile = discardPile; }

    public String getStoryStripFragmentIds() { return storyStripFragmentIds; }
    public void setStoryStripFragmentIds(String storyStripFragmentIds) { this.storyStripFragmentIds = storyStripFragmentIds; }

    public String getCurrentFamily() { return currentFamily; }
    public void setCurrentFamily(String currentFamily) { this.currentFamily = currentFamily; }

    public Integer getCurrentIntensity() { return currentIntensity; }
    public void setCurrentIntensity(Integer currentIntensity) { this.currentIntensity = currentIntensity; }

    public String getPendingEffects() { return pendingEffects; }
    public void setPendingEffects(String pendingEffects) { this.pendingEffects = pendingEffects; }

    public Long getWinnerId() { return winnerId; }
    public void setWinnerId(Long winnerId) { this.winnerId = winnerId; }

    public Long getActiveJudgmentId() { return activeJudgmentId; }
    public void setActiveJudgmentId(Long activeJudgmentId) { this.activeJudgmentId = activeJudgmentId; }

    public String getActiveJudgmentTarget() { return activeJudgmentTarget; }
    public void setActiveJudgmentTarget(String activeJudgmentTarget) { this.activeJudgmentTarget = activeJudgmentTarget; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
