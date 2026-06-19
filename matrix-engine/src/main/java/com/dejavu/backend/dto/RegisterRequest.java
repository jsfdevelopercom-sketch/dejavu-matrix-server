package com.dejavu.backend.dto;

public class RegisterRequest {
    private String username;
    private String language;
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
}
