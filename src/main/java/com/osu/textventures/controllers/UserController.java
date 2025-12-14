package com.osu.textventures.controllers;

import com.osu.textventures.models.User;
import com.osu.textventures.services.UserService;
import com.osu.textventures.utils.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;
    private final JwtUtil jwtUtil;

    public UserController(UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        try {
            String username = body.get("username");
            String password = body.get("password");
            String role = body.getOrDefault("role", "player");
            logger.info("POST /api/register - username: {}", username);

            if (username == null || username.trim().isEmpty()) {
                logger.warn("Registration failed - missing username");
                return ResponseEntity.badRequest().body(Map.of("error", "Username is required"));
            }
            if (password == null || password.length() < 6) {
                logger.warn("Registration failed - invalid password for username: {}", username);
                return ResponseEntity.badRequest().body(Map.of("error", "Password must be at least 6 characters"));
            }

            String id = userService.createUser(username, password, role);
            String token = jwtUtil.generateToken(id, username);

            logger.info("User registered successfully: {}", username);
            return ResponseEntity.ok(Map.of(
                    "message", "User registered",
                    "id", id,
                    "username", username,
                    "role", role,
                    "token", token
            ));
        } catch (Exception e) {
            logger.error("Registration error for username {}: {}", body.get("username"), e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        try {
            String username = body.get("username");
            String password = body.get("password");
            logger.info("POST /api/login - username: {}", username);

            if (username == null || username.trim().isEmpty()) {
                logger.warn("Login failed - missing username");
                return ResponseEntity.badRequest().body(Map.of("error", "Username is required"));
            }
            if (password == null || password.trim().isEmpty()) {
                logger.warn("Login failed - missing password for username: {}", username);
                return ResponseEntity.badRequest().body(Map.of("error", "Password is required"));
            }

            User user = userService.login(username, password);
            String token = jwtUtil.generateToken(user.getId(), user.getUsername());

            logger.info("User logged in successfully: {}", username);
            return ResponseEntity.ok(Map.of(
                    "message", "Login successful",
                    "username", user.getUsername(),
                    "role", user.getRole(),
                    "id", user.getId(),
                    "token", token
            ));
        } catch (Exception e) {
            logger.error("Login error for username {}: {}", body.get("username"), e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
