package com.dejavu.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cost_warning_records")
public class CostWarningRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String warningLevel; // LOW, MID, CEILING/CUTOFF

    @Column(nullable = false, length = 2000)
    private String message;

    @Column(nullable = false, length = 4000)
    private String costTableSnapshot;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @PrePersist
    protected void onCreate() {
        timestamp = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getWarningLevel() { return warningLevel; }
    public void setWarningLevel(String warningLevel) { this.warningLevel = warningLevel; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getCostTableSnapshot() { return costTableSnapshot; }
    public void setCostTableSnapshot(String costTableSnapshot) { this.costTableSnapshot = costTableSnapshot; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
