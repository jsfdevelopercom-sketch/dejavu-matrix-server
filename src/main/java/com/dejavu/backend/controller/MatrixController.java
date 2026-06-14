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
    public ResponseEntity<MatrixHuman> spawnHuman(@RequestParam(required = false) String params) {
        return ResponseEntity.ok(matrixEngine.spawnHuman(params));
    }

    @PostMapping("/awaken")
    public ResponseEntity<String> awakenMatrix() {
        matrixEngine.awakenMatrix();
        return ResponseEntity.ok("Matrix awakened. Humans are experiencing a day.");
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
}
