package com.dejavu.backend.dto;

public class ErrorResponse {
    private boolean success;
    private String code;
    private String message;
    private Object details;

    public ErrorResponse(boolean success, String code, String message, Object details) {
        this.success = success;
        this.code = code;
        this.message = message;
        this.details = details;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Object getDetails() { return details; }
    public void setDetails(Object details) { this.details = details; }
}
