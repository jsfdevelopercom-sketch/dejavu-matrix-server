package com.dejavu.backend.model.game;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "confession_game_content")
public class ConfessionGameContent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long confessionId;
    private String status;
    private String title;
    @Column(columnDefinition = "TEXT")
    private String fullRevealText;
    @Column(columnDefinition = "TEXT")
    private String anonymizedSummary;
    @Column(columnDefinition = "TEXT")
    private String demographics;
    @Column(columnDefinition = "TEXT")
    private String motive;
    @Column(columnDefinition = "TEXT")
    private String emotionalSignificance;
    private String createdByModel;
    private Double qualityScore;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "gameContent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ConfessionFragment> fragments = new ArrayList<>();

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

    public Long getConfessionId() { return confessionId; }
    public void setConfessionId(Long confessionId) { this.confessionId = confessionId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getFullRevealText() { return fullRevealText; }
    public void setFullRevealText(String fullRevealText) { this.fullRevealText = fullRevealText; }

    public String getAnonymizedSummary() { return anonymizedSummary; }
    public void setAnonymizedSummary(String anonymizedSummary) { this.anonymizedSummary = anonymizedSummary; }

    public String getDemographics() { return demographics; }
    public void setDemographics(String demographics) { this.demographics = demographics; }

    public String getMotive() { return motive; }
    public void setMotive(String motive) { this.motive = motive; }

    public String getEmotionalSignificance() { return emotionalSignificance; }
    public void setEmotionalSignificance(String emotionalSignificance) { this.emotionalSignificance = emotionalSignificance; }

    public String getCreatedByModel() { return createdByModel; }
    public void setCreatedByModel(String createdByModel) { this.createdByModel = createdByModel; }

    public Double getQualityScore() { return qualityScore; }
    public void setQualityScore(Double qualityScore) { this.qualityScore = qualityScore; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public List<ConfessionFragment> getFragments() { return fragments; }
    public void setFragments(List<ConfessionFragment> fragments) { this.fragments = fragments; }
}
