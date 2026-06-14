package com.dejavu.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "guess_attempts")
public class GuessAttempt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long roomSessionId;
    
    @Column(columnDefinition = "TEXT")
    private String guessText;
    
    private int accuracy;
    private String tier; // MISS, WEAK, CLOSE, CORRECT
    
    private int pointsAwarded;
    private int coinsAwarded;
    
    @Column(columnDefinition = "TEXT")
    private String angelFeedback;
    
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getRoomSessionId() { return roomSessionId; }
    public void setRoomSessionId(Long roomSessionId) { this.roomSessionId = roomSessionId; }

    public String getGuessText() { return guessText; }
    public void setGuessText(String guessText) { this.guessText = guessText; }

    public int getAccuracy() { return accuracy; }
    public void setAccuracy(int accuracy) { this.accuracy = accuracy; }

    public String getTier() { return tier; }
    public void setTier(String tier) { this.tier = tier; }

    public int getPointsAwarded() { return pointsAwarded; }
    public void setPointsAwarded(int pointsAwarded) { this.pointsAwarded = pointsAwarded; }

    public int getCoinsAwarded() { return coinsAwarded; }
    public void setCoinsAwarded(int coinsAwarded) { this.coinsAwarded = coinsAwarded; }

    public String getAngelFeedback() { return angelFeedback; }
    public void setAngelFeedback(String angelFeedback) { this.angelFeedback = angelFeedback; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
