package com.dejavu.backend.controller;

import com.dejavu.backend.model.AppSession;
import com.dejavu.backend.repository.AppSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    @Autowired
    private AppSessionRepository sessionRepository;

    @Autowired
    private com.dejavu.backend.repository.UserAccountRepository userRepository;

    @PostMapping("/session")
    public ResponseEntity<Void> logSession(@RequestBody AppSession session) {
        if (session.getUserId() != null && userRepository.existsById(session.getUserId())) {
            sessionRepository.save(session);
        }
        return ResponseEntity.ok().build();
    }
}
