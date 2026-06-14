package com.dejavu.backend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;

/**
 * Stores dynamic AI prompts so they can be edited live via the Admin App
 * without requiring a backend recompilation or redeployment.
 */
@Entity
@Table(name = "prompt_configs")
public class PromptConfig {
    @Id
    private String promptKey;
    
    @Column(columnDefinition = "TEXT")
    private String promptContent;

    public PromptConfig() {}

    public PromptConfig(String promptKey, String promptContent) {
        this.promptKey = promptKey;
        this.promptContent = promptContent;
    }

    public String getPromptKey() { return promptKey; }
    public void setPromptKey(String promptKey) { this.promptKey = promptKey; }

    public String getPromptContent() { return promptContent; }
    public void setPromptContent(String promptContent) { this.promptContent = promptContent; }
}
