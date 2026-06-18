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
        return ResponseEntity.ok(humanRepository.findAll());
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

    @GetMapping(value = "/avatars/{filename}", produces = org.springframework.http.MediaType.IMAGE_PNG_VALUE)
    public @ResponseBody byte[] getAvatar(@PathVariable String filename) throws java.io.IOException {
        java.io.File file = new java.io.File("data/avatars/" + filename);
        if (file.exists()) {
            return java.nio.file.Files.readAllBytes(file.toPath());
        }
        return null;
    }
}
