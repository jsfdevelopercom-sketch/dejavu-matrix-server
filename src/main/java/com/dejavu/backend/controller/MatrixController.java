package com.dejavu.backend.controller;

import com.dejavu.backend.ai.MatrixEngine;
import com.dejavu.backend.model.MatrixHuman;
import com.dejavu.backend.repository.MatrixHumanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/matrix")
public class MatrixController {

    @Autowired
    private MatrixEngine matrixEngine;
    
    @Autowired
    private MatrixHumanRepository humanRepository;

    @GetMapping("/humans")
    public ResponseEntity<List<MatrixHuman>> getHumans() {
        List<MatrixHuman> humans = humanRepository.findAll();
        
        // Auto-heal: Weed out Agent Smiths manually and map generated avatars
        humans.removeIf(h -> {
            if (h.getName() != null && h.getName().toLowerCase().contains("agent smith")) {
                humanRepository.delete(h);
                return true;
            }
            if (h.getAvatarUrl() == null || h.getAvatarUrl().trim().isEmpty() || h.getAvatarUrl().equals("null")) {
                String safeName = h.getName().toLowerCase().replace(" ", "_").replace("'", "").replace(".", "");
                java.io.File f = new java.io.File("data/avatars/" + safeName + ".png");
                if (f.exists()) {
                    h.setAvatarUrl("/api/matrix/avatars/" + safeName + ".png");
                    humanRepository.save(h);
                }
            }
            return false;
        });
        
        return ResponseEntity.ok(humans);
    }

    @PostMapping("/humans")
    public ResponseEntity<MatrixHuman> spawnHuman(@RequestParam(required = false) String name, @RequestParam(required = false) String params) {
        return ResponseEntity.ok(matrixEngine.spawnHuman(name, params));
    }

    @PostMapping("/awaken")
    public ResponseEntity<String> awakenMatrix() {
        new Thread(() -> {
            try {
                matrixEngine.awakenMatrix();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
        return ResponseEntity.ok("Matrix awakened in background. Humans are experiencing a day.");
    }
    
    @PostMapping("/clear-avatars")
    public ResponseEntity<String> clearAvatars() {
        List<MatrixHuman> humans = humanRepository.findAll();
        for (MatrixHuman h : humans) {
            h.setAvatarUrl(null);
            humanRepository.save(h);
        }
        
        java.io.File dir = new java.io.File("data/avatars");
        if (dir.exists() && dir.isDirectory()) {
            for (java.io.File f : dir.listFiles()) {
                if (f.getName().endsWith(".png")) {
                    f.delete();
                }
            }
        }
        
        matrixEngine.processAllIncompleteConfessions();
        return ResponseEntity.ok("Avatars cleared, old files deleted, and regeneration started for full-body shots.");
    }

    @PostMapping("/call")
    public ResponseEntity<String> phoneCall(@RequestParam Long callerId, @RequestParam Long receiverId) {
        return ResponseEntity.ok(matrixEngine.phoneCall(callerId, receiverId));
    }

    @PostMapping("/humans/{id}/confess")
    public ResponseEntity<String> forceConfession(@PathVariable Long id) {
        return ResponseEntity.ok(matrixEngine.forceConfession(id));
    }

    @PostMapping("/humans/{id}/inject")
    public ResponseEntity<String> injectThought(@PathVariable Long id, @RequestParam String thought) {
        return ResponseEntity.ok(matrixEngine.injectThought(id, thought));
    }

    @PostMapping("/world-event")
    public ResponseEntity<String> injectWorldEvent(
            @RequestParam String event, 
            @RequestParam(required = false) List<Long> involvedHumanIds) {
        return ResponseEntity.ok(matrixEngine.injectWorldEvent(event, involvedHumanIds));
    }

    @PostMapping("/humans/{id}/chat")
    public ResponseEntity<String> chatWithHuman(@PathVariable Long id, @RequestParam String message) {
        return ResponseEntity.ok(matrixEngine.chatWithHuman(id, message));
    }

    @PostMapping("/town-square/chat-turn")
    public ResponseEntity<String> townSquareTurn(@RequestParam String chatHistory, @RequestParam(required = false) String godMessage) {
        return ResponseEntity.ok(matrixEngine.townSquareTurn(chatHistory, godMessage));
    }

    @PostMapping("/process-all-confessions")
    public ResponseEntity<String> processAllConfessions() {
        return ResponseEntity.ok(matrixEngine.processAllIncompleteConfessions());
    }

    @GetMapping(value = "/avatars/{filename}", produces = org.springframework.http.MediaType.IMAGE_PNG_VALUE)
    public @ResponseBody byte[] getAvatar(@PathVariable String filename) throws java.io.IOException {
        java.io.File file = new java.io.File("data/avatars/" + filename);
        if (file.exists()) {
            return java.nio.file.Files.readAllBytes(file.toPath());
        }
        return null;
    }

    @Autowired
    private com.dejavu.backend.repository.RamonNotificationRepository notificationRepository;

    @GetMapping("/notifications")
    public ResponseEntity<List<com.dejavu.backend.model.RamonNotification>> getNotifications() {
        List<com.dejavu.backend.model.RamonNotification> notifs = notificationRepository.findByIsReadFalse();
        for (com.dejavu.backend.model.RamonNotification notif : notifs) {
            notif.setRead(true);
            notificationRepository.save(notif);
        }
        return ResponseEntity.ok(notifs);
    }
}
