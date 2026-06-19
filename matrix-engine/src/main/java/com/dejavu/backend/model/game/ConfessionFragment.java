package com.dejavu.backend.model.game;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "confession_fragment")
public class ConfessionFragment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_content_id")
    @JsonIgnore
    private ConfessionGameContent gameContent;

    private Integer fragmentOrder;
    private String emotionFamily;
    private String emotionShade;
    private Integer intensity;

    @Column(columnDefinition = "TEXT")
    private String fragmentText;
    
    @Column(columnDefinition = "TEXT")
    private String shortFragmentText;
    
    @Column(columnDefinition = "TEXT")
    private String fullFragmentText;

    private String artAssetId;

    @OneToMany(mappedBy = "fragment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JudgmentQuestion> judgments = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ConfessionGameContent getGameContent() { return gameContent; }
    public void setGameContent(ConfessionGameContent gameContent) { this.gameContent = gameContent; }

    public Integer getFragmentOrder() { return fragmentOrder; }
    public void setFragmentOrder(Integer fragmentOrder) { this.fragmentOrder = fragmentOrder; }

    public String getEmotionFamily() { return emotionFamily; }
    public void setEmotionFamily(String emotionFamily) { this.emotionFamily = emotionFamily; }

    public String getEmotionShade() { return emotionShade; }
    public void setEmotionShade(String emotionShade) { this.emotionShade = emotionShade; }

    public Integer getIntensity() { return intensity; }
    public void setIntensity(Integer intensity) { this.intensity = intensity; }

    public String getFragmentText() { return fragmentText; }
    public void setFragmentText(String fragmentText) { this.fragmentText = fragmentText; }

    public String getShortFragmentText() { return shortFragmentText; }
    public void setShortFragmentText(String shortFragmentText) { this.shortFragmentText = shortFragmentText; }

    public String getFullFragmentText() { return fullFragmentText; }
    public void setFullFragmentText(String fullFragmentText) { this.fullFragmentText = fullFragmentText; }

    public String getArtAssetId() { return artAssetId; }
    public void setArtAssetId(String artAssetId) { this.artAssetId = artAssetId; }

    public List<JudgmentQuestion> getJudgments() { return judgments; }
    public void setJudgments(List<JudgmentQuestion> judgments) { this.judgments = judgments; }
}
