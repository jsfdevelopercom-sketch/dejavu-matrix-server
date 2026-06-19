package com.dejavu.backend.model;
import jakarta.persistence.*;
@Entity
public class RamonNotification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long humanId;
    private String humanName;
    private String message;
    private boolean isRead = false;
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getHumanId() { return humanId; }
    public void setHumanId(Long humanId) { this.humanId = humanId; }
    public String getHumanName() { return humanName; }
    public void setHumanName(String humanName) { this.humanName = humanName; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }
}
