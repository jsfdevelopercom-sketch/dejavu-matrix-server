package com.dejavu.backend.controller;

import com.dejavu.backend.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.NoSuchElementException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoSuchElementException ex) {
        String msg = ex.getMessage();
        if (msg == null) msg = "Resource not found";
        return buildResponse("NOT_FOUND", msg, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        String msg = ex.getMessage();
        if (msg != null && msg.contains("Username is already taken")) {
            return buildResponse("USERNAME_TAKEN", msg, HttpStatus.CONFLICT);
        }
        return buildResponse("BAD_REQUEST", msg != null ? msg : "Invalid request", HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex) {
        String msg = ex.getMessage();
        String code = "BAD_REQUEST";
        if (msg != null) {
            if (msg.contains("Insufficient coins")) {
                code = "INSUFFICIENT_COINS";
            } else if (msg.contains("Maximum clues reached")) {
                code = "MAX_CLUES_REACHED";
            }
        }
        return buildResponse(code, msg != null ? msg : "Invalid state", HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        return buildResponse("INTERNAL_ERROR", ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<ErrorResponse> buildResponse(String code, String message, HttpStatus status) {
        ErrorResponse error = new ErrorResponse(false, code, message, null);
        return new ResponseEntity<>(error, status);
    }
}
