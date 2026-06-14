package com.dejavu.backend.controller;

import com.dejavu.backend.dto.*;
import com.dejavu.backend.service.GameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/game")
public class GameController {

    @Autowired
    private GameService gameService;

    @PostMapping("/room/start")
    public ResponseEntity<StartRoomResponse> startRoom(@RequestBody StartRoomRequest request) {
        return ResponseEntity.ok(gameService.startRoom(request.getUserId()));
    }

    @PostMapping("/room/{sessionId}/ready")
    public ResponseEntity<ClueResponse> ready(@PathVariable Long sessionId, @RequestBody ReadyRequest request) {
        return ResponseEntity.ok(gameService.getReadyClue(sessionId));
    }

    @PostMapping("/room/{sessionId}/guess")
    public ResponseEntity<GuessResponse> guess(@PathVariable Long sessionId, @RequestBody GuessRequest request) {
        return ResponseEntity.ok(gameService.processGuess(sessionId, request.getGuessText()));
    }

    @PostMapping("/room/{sessionId}/next-clue")
    public ResponseEntity<ClueResponse> nextClue(@PathVariable Long sessionId, @RequestBody NextClueRequest request) {
        try {
            return ResponseEntity.ok(gameService.getNextClue(sessionId, request));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/room/{sessionId}/expire")
    public ResponseEntity<GuessResponse> expireRoom(@PathVariable Long sessionId) {
        return ResponseEntity.ok(gameService.expireRoom(sessionId));
    }

    @PostMapping("/room/{sessionId}/timer/pause")
    public ResponseEntity<Void> pauseTimer(@PathVariable Long sessionId) {
        gameService.pauseTimer(sessionId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/room/{sessionId}/timer/resume")
    public ResponseEntity<Void> resumeTimer(@PathVariable Long sessionId) {
        gameService.resumeTimer(sessionId);
        return ResponseEntity.ok().build();
    }

    @Autowired
    private com.dejavu.backend.ai.DarkArchangelInterviewEngine interviewEngine;

    @PostMapping("/confession/chat")
    public ResponseEntity<com.dejavu.backend.ai.DarkArchangelInterviewEngine.ChatResponse> chat(
            @RequestBody com.dejavu.backend.ai.DarkArchangelInterviewEngine.ChatRequest request) {
        int max = com.dejavu.backend.controller.AdminController.getGlobalMaxQuestions();
        return ResponseEntity.ok(interviewEngine.interactiveChat(request, max));
    }

    @PostMapping("/confession/chat/filler")
    public ResponseEntity<String> chatFiller(
            @RequestBody com.dejavu.backend.ai.DarkArchangelInterviewEngine.ChatRequest request) {
        return ResponseEntity.ok(interviewEngine.generateFiller(request));
    }
}
