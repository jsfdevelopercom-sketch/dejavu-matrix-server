package com.dejavu.backend.model;

import jakarta.persistence.Entity;

@Entity
public class MatrixAngel extends MatrixHuman {
    
    private int powerLevel;
    private String domain; // E.g. "Judgment", "Fate"

    public int getPowerLevel() { return powerLevel; }
    public void setPowerLevel(int powerLevel) { this.powerLevel = powerLevel; }

    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }
}
