package com.dejavu.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "coin_transactions")
public class CoinTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    
    private String type; // SIGNUP_BONUS, ROOM_WIN, EXTRA_CLUE_SPEND, PURCHASE, ADMIN_GRANT
    
    private int amount;
    private int balanceAfter;
    
    private Long roomSessionId;
    private String purchasePack;
    
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public int getAmount() { return amount; }
    public void setAmount(int amount) { this.amount = amount; }

    public int getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(int balanceAfter) { this.balanceAfter = balanceAfter; }

    public Long getRoomSessionId() { return roomSessionId; }
    public void setRoomSessionId(Long roomSessionId) { this.roomSessionId = roomSessionId; }

    public String getPurchasePack() { return purchasePack; }
    public void setPurchasePack(String purchasePack) { this.purchasePack = purchasePack; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
