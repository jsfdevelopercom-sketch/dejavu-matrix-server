package com.dejavu.backend.controller.game;

import com.dejavu.backend.dto.game.GameSessionStateResponse;
import com.dejavu.backend.dto.game.PlayCardRequest;
import com.dejavu.backend.dto.game.AnswerJudgmentRequest;
import com.dejavu.backend.service.game.GameSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/game/sessions")
public class GameSessionController {

    @Autowired
    private GameSessionService gameSessionService;

    @PostMapping("/ai/start")
    public ResponseEntity<?> startAiMatch(@RequestParam Long playerId) {
        try {
            return ResponseEntity.ok(gameSessionService.startAiMatch(playerId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<?> getSession(@PathVariable Long sessionId) {
        try {
            return ResponseEntity.ok(gameSessionService.getState(sessionId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{sessionId}/play-card")
    public ResponseEntity<?> playCard(@PathVariable Long sessionId, @RequestBody PlayCardRequest request) {
        try {
            return ResponseEntity.ok(gameSessionService.playCard(sessionId, request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{sessionId}/draw-card")
    public ResponseEntity<?> drawCard(@PathVariable Long sessionId) {
        try {
            return ResponseEntity.ok(gameSessionService.drawCard(sessionId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{sessionId}/answer-judgment")
    public ResponseEntity<?> answerJudgment(@PathVariable Long sessionId, @RequestBody AnswerJudgmentRequest request) {
        try {
            return ResponseEntity.ok(gameSessionService.answerJudgment(sessionId, request.getAnswer()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
