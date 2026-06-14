package com.dejavu.backend.controller;

import com.dejavu.backend.dto.*;
import com.dejavu.backend.model.UserAccount;
import com.dejavu.backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @PostMapping("/check-username")
    public ResponseEntity<UsernameCheckResponse> checkUsername(@RequestBody UsernameCheckRequest request) {
        boolean available = userService.isUsernameAvailable(request.getUsername());
        String reason = available ? "Available" : "Username is already taken";
        return ResponseEntity.ok(new UsernameCheckResponse(available, reason));
    }

    @PostMapping("/register")
    public ResponseEntity<UserAccountDto> register(@RequestBody RegisterRequest request) {
        try {
            UserAccount user = userService.registerUser(request.getUsername(), request.getLanguage());
            return ResponseEntity.ok(new UserAccountDto(user));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/login")
    public ResponseEntity<UserAccountDto> login(@RequestBody RegisterRequest request) {
        // Simple login for phase 1 - just return the user if they exist
        return userService.getUserByUsername(request.getUsername())
                .map(u -> ResponseEntity.ok(new UserAccountDto(u)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/language/{userId}")
    public ResponseEntity<Void> updateLanguage(@PathVariable Long userId, @RequestBody RegisterRequest request) {
        userService.updateLanguage(userId, request.getLanguage());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/set-gamename/{userId}")
    public ResponseEntity<UserAccountDto> setGameName(@PathVariable Long userId, @RequestBody RegisterRequest request) {
        return userService.getUser(userId).map(user -> {
            user.setGameName(request.getUsername());
            userService.saveUser(user);
            return ResponseEntity.ok(new UserAccountDto(user));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/onboarding-complete")
    public ResponseEntity<UserAccountDto> onboardingComplete(@RequestBody java.util.Map<String, Long> request) {
        Long userId = request.get("userId");
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }
        userService.markOnboardingCompleted(userId);
        return userService.getUser(userId)
                .map(u -> ResponseEntity.ok(new UserAccountDto(u)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/google")
    public ResponseEntity<UserAccountDto> googleLogin(@RequestBody GoogleLoginRequest request) {
        try {
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            String url = "https://oauth2.googleapis.com/tokeninfo?id_token=" + request.getIdToken();
            java.util.Map<String, Object> response = restTemplate.getForObject(url, java.util.Map.class);
            
            if (response != null && response.containsKey("email")) {
                String email = (String) response.get("email");
                String name = (String) response.getOrDefault("name", email.split("@")[0]);
                
                return userService.getUserByUsername(email)
                        .map(u -> ResponseEntity.ok(new UserAccountDto(u)))
                        .orElseGet(() -> {
                            UserAccount newUser = userService.registerUser(email, request.getLanguage());
                            return ResponseEntity.ok(new UserAccountDto(newUser));
                        });
            } else {
                return ResponseEntity.status(401).build();
            }
        } catch (Exception e) {
            System.err.println("Google Auth verification failed: " + e.getMessage());
            return ResponseEntity.status(401).build();
        }
    }
}
