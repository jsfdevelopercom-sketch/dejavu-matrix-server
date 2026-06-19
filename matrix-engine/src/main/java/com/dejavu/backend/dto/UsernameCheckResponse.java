package com.dejavu.backend.dto;

public class UsernameCheckResponse {
    private boolean available;
    private String reason;

    public UsernameCheckResponse(boolean available, String reason) {
        this.available = available;
        this.reason = reason;
    }
    public boolean isAvailable() { return available; }
    public String getReason() { return reason; }
}
