package com.osu.textventures.controllers;

import com.osu.textventures.services.CombatService;
import com.osu.textventures.services.GameService;
import com.osu.textventures.models.GameState;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/game")
@CrossOrigin
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    private String getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof String) {
            return (String) authentication.getPrincipal();
        }
        throw new IllegalStateException("User not authenticated");
    }

    @PostMapping("/start")
    public ResponseEntity<?> startGame(@RequestBody Map<String, String> body) {
        try {
            String userId = getAuthenticatedUserId();
            String characterName = body.get("characterName");

            if (characterName == null || characterName.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "characterName is required."));
            }

            GameState gameState = gameService.startGame(userId, characterName);
            return ResponseEntity.ok(gameState);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to start game: " + e.getMessage()));
        }
    }

    @GetMapping("/state")
    public ResponseEntity<?> getGameState() {
        try {
            String userId = getAuthenticatedUserId();
            GameState gameState = gameService.getGameState(userId);
            return ResponseEntity.ok(gameState);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to get game state: " + e.getMessage()));
        }
    }

    @PostMapping("/choice")
    public ResponseEntity<?> processChoice(@RequestBody Map<String, String> body) {
        try {
            String userId = getAuthenticatedUserId();
            String choiceId = body.get("choiceId");

            if (choiceId == null || choiceId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "choiceId is required."));
            }

            GameState gameState = gameService.processChoice(userId, choiceId);
            return ResponseEntity.ok(gameState);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to process choice: " + e.getMessage()));
        }
    }

    @PostMapping("/combat")
    public ResponseEntity<?> processCombatAction(@RequestBody Map<String, String> body) {
        try {
            String userId = getAuthenticatedUserId();
            String actionStr = body.get("action");

            if (actionStr == null || actionStr.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "action is required."));
            }

            CombatService.CombatAction action = CombatService.CombatAction.valueOf(actionStr.toUpperCase());
            GameState gameState = gameService.processCombatAction(userId, action);
            return ResponseEntity.ok(gameState);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to process combat action: " + e.getMessage()));
        }
    }

    @DeleteMapping("/reset")
    public ResponseEntity<?> resetGame() {
        try {
            String userId = getAuthenticatedUserId();
            gameService.resetGame(userId);
            return ResponseEntity.ok(Map.of("message", "Game reset successfully"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to reset game: " + e.getMessage()));
        }
    }
}

