package com.osu.textventures.controllers;

import com.osu.textventures.services.GameService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/game")
@CrossOrigin
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @PostMapping("/start")
    public ResponseEntity<?> startGame(@RequestBody Map<String, String> body) {
        try {
            String userId = body.get("userId");
            String characterName = body.get("characterName");

            if (userId == null || characterName == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "userId and characterName are required."));
            }

            GameService.GameState gameState = gameService.startGame(userId, characterName);
            return ResponseEntity.ok(gameState);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to start game: " + e.getMessage()));
        }
    }

    @GetMapping("/state")
    public ResponseEntity<?> getGameState(@RequestParam String userId) {
        try {
            GameService.GameState gameState = gameService.getGameState(userId);
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
            String userId = body.get("userId");
            String choiceId = body.get("choiceId");

            if (userId == null || choiceId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "userId and choiceId are required."));
            }

            GameService.GameState gameState = gameService.processChoice(userId, choiceId);
            return ResponseEntity.ok(gameState);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to process choice: " + e.getMessage()));
        }
    }
    @DeleteMapping("/reset")
    public ResponseEntity<?> resetGame(@RequestParam String userId) {
        try {
            gameService.resetGame(userId);
            return ResponseEntity.ok(Map.of("message", "Game reset successfully"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to reset game: " + e.getMessage()));
        }
    }
}

