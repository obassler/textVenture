package com.osu.textventures.controllers;

import com.osu.textventures.services.CombatService;
import com.osu.textventures.services.GameService;
import com.osu.textventures.models.GameState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/game")
@CrossOrigin
public class GameController {

    private static final Logger logger = LoggerFactory.getLogger(GameController.class);

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
            logger.info("POST /api/game/start - user: {}, character: {}", userId, characterName);

            if (characterName == null || characterName.trim().isEmpty()) {
                logger.warn("Start game failed - missing character name for user: {}", userId);
                return ResponseEntity.badRequest().body(Map.of("error", "characterName is required."));
            }

            GameState gameState = gameService.startGame(userId, characterName);
            logger.info("Game started successfully for user: {}", userId);
            return ResponseEntity.ok(gameState);
        } catch (IllegalArgumentException e) {
            logger.warn("Start game failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Start game error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to start game: " + e.getMessage()));
        }
    }

    @GetMapping("/state")
    public ResponseEntity<?> getGameState() {
        try {
            String userId = getAuthenticatedUserId();
            logger.debug("GET /api/game/state - user: {}", userId);
            GameState gameState = gameService.getGameState(userId);
            return ResponseEntity.ok(gameState);
        } catch (IllegalArgumentException e) {
            logger.warn("Get game state failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Get game state error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to get game state: " + e.getMessage()));
        }
    }

    @PostMapping("/choice")
    public ResponseEntity<?> processChoice(@RequestBody Map<String, String> body) {
        try {
            String userId = getAuthenticatedUserId();
            String choiceId = body.get("choiceId");
            logger.info("POST /api/game/choice - user: {}, choice: {}", userId, choiceId);

            if (choiceId == null || choiceId.trim().isEmpty()) {
                logger.warn("Process choice failed - missing choiceId for user: {}", userId);
                return ResponseEntity.badRequest().body(Map.of("error", "choiceId is required."));
            }

            GameState gameState = gameService.processChoice(userId, choiceId);
            return ResponseEntity.ok(gameState);
        } catch (IllegalArgumentException e) {
            logger.warn("Process choice failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Process choice error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to process choice: " + e.getMessage()));
        }
    }

    @PostMapping("/combat")
    public ResponseEntity<?> processCombatAction(@RequestBody Map<String, String> body) {
        try {
            String userId = getAuthenticatedUserId();
            String actionStr = body.get("action");
            logger.info("POST /api/game/combat - user: {}, action: {}", userId, actionStr);

            if (actionStr == null || actionStr.trim().isEmpty()) {
                logger.warn("Combat action failed - missing action for user: {}", userId);
                return ResponseEntity.badRequest().body(Map.of("error", "action is required."));
            }

            CombatService.CombatAction action = CombatService.CombatAction.valueOf(actionStr.toUpperCase());
            GameState gameState = gameService.processCombatAction(userId, action);
            return ResponseEntity.ok(gameState);
        } catch (IllegalArgumentException e) {
            logger.warn("Combat action failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Combat action error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to process combat action: " + e.getMessage()));
        }
    }

    @DeleteMapping("/reset")
    public ResponseEntity<?> resetGame() {
        try {
            String userId = getAuthenticatedUserId();
            logger.info("DELETE /api/game/reset - user: {}", userId);
            gameService.resetGame(userId);
            logger.info("Game reset successfully for user: {}", userId);
            return ResponseEntity.ok(Map.of("message", "Game reset successfully"));
        } catch (Exception e) {
            logger.error("Reset game error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to reset game: " + e.getMessage()));
        }
    }
}

