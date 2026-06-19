package com.dejavu.backend.model.game;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "judgment_question")
public class JudgmentQuestion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fragment_id")
    @JsonIgnore
    private ConfessionFragment fragment;

    @Column(columnDefinition = "TEXT")
    private String text;

    private Boolean correctAnswer;
    private String difficulty; // EASY, MEDIUM, HARD
    
    @Column(columnDefinition = "TEXT")
    @JsonIgnore
    private String explanationForBackendOnly;
    
    private String emotionalAxis;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ConfessionFragment getFragment() { return fragment; }
    public void setFragment(ConfessionFragment fragment) { this.fragment = fragment; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public Boolean getCorrectAnswer() { return correctAnswer; }
    public void setCorrectAnswer(Boolean correctAnswer) { this.correctAnswer = correctAnswer; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public String getExplanationForBackendOnly() { return explanationForBackendOnly; }
    public void setExplanationForBackendOnly(String explanationForBackendOnly) { this.explanationForBackendOnly = explanationForBackendOnly; }

    public String getEmotionalAxis() { return emotionalAxis; }
    public void setEmotionalAxis(String emotionalAxis) { this.emotionalAxis = emotionalAxis; }
}
