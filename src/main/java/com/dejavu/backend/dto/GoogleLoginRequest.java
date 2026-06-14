package com.dejavu.backend.dto;

public class GoogleLoginRequest {
    private String idToken;
    private String language;

    public String getIdToken() { return idToken; }
    public void setIdToken(String idToken) { this.idToken = idToken; }
    
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
}
