package com.osu.textventures.services;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import com.osu.textventures.models.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final Firestore db = FirestoreClient.getFirestore();
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public String createUser(String username, String password, String role) throws ExecutionException, InterruptedException {
        logger.info("Creating new user: {}", username);

        CollectionReference users = db.collection("users");
        ApiFuture<QuerySnapshot> query = users.whereEqualTo("username", username).get();
        if (!query.get().isEmpty()) {
            logger.warn("Registration failed - username already exists: {}", username);
            throw new IllegalArgumentException("Username already exists");
        }

        String id = db.collection("users").document().getId();
        String passwordHash = passwordEncoder.encode(password);

        User user = new User(id, username, passwordHash, role, null);

        db.collection("users").document(id).set(user);
        logger.info("User created successfully: {} with role: {}", username, role);
        return id;
    }

    public User login(String username, String password) throws ExecutionException, InterruptedException {
        logger.info("Login attempt for user: {}", username);

        CollectionReference users = db.collection("users");
        ApiFuture<QuerySnapshot> query = users.whereEqualTo("username", username).get();

        QuerySnapshot snapshot = query.get();
        if (snapshot.isEmpty()) {
            logger.warn("Login failed - user not found: {}", username);
            throw new IllegalArgumentException("User not found");
        }

        User user = snapshot.getDocuments().get(0).toObject(User.class);
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            logger.warn("Login failed - invalid password for user: {}", username);
            throw new IllegalArgumentException("Invalid password");
        }

        logger.info("User logged in successfully: {}", username);
        return user;
    }
}
