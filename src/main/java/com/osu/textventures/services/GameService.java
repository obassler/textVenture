package com.osu.textventures.services;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import com.osu.textventures.models.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Service
public class GameService {

    private final Firestore db = FirestoreClient.getFirestore();

    private <T> T getDocument(String collectionName, String documentId, Class<T> type) throws ExecutionException, InterruptedException {
        DocumentReference docRef = db.collection(collectionName).document(documentId);
        ApiFuture<DocumentSnapshot> future = docRef.get();
        DocumentSnapshot document = future.get();
        if (document.exists()) {
            return document.toObject(type);
        } else {
            return null;
        }
    }

    private <T> void saveDocument(String collectionName, String documentId, T data) throws ExecutionException, InterruptedException {
        db.collection(collectionName).document(documentId).set(data).get();
    }

    public PlayerCharacter getPlayerCharacter(String userId) throws ExecutionException, InterruptedException {
        return getDocument("playerCharacters", userId, PlayerCharacter.class);
    }

    public Location getLocation(String locationId) throws ExecutionException, InterruptedException {
        return getDocument("locations", locationId, Location.class);
    }

    public Enemy getEnemy(String enemyId) throws ExecutionException, InterruptedException {
        return getDocument("enemies", enemyId, Enemy.class);
    }

    public GameState startGame(String userId, String characterName) throws ExecutionException, InterruptedException {
        PlayerCharacter existingCharacter = getPlayerCharacter(userId);
        if (existingCharacter != null) {
            throw new IllegalArgumentException("Player character already exists for this user.");
        }

        PlayerCharacter newCharacter = new PlayerCharacter(
                userId, characterName, 1, 0, new ArrayList<>(), 10, 100
        );

        saveDocument("playerCharacters", userId, newCharacter);

        Location startLocation = getLocation(newCharacter.getCurrentLocationId());
        if (startLocation == null) {
            throw new IllegalStateException("Starting location not found.");
        }

        newCharacter.getGameHistory().add(startLocation.getDescription());
        saveDocument("playerCharacters", userId, newCharacter);

        return new GameState(newCharacter, startLocation.getDescription(), startLocation.getAvailableChoices());
    }

    public GameState getGameState(String userId) throws ExecutionException, InterruptedException {
        PlayerCharacter player = getPlayerCharacter(userId);
        if (player == null) {
            throw new IllegalArgumentException("Player character not found. Please start a new game.");
        }

        Location currentLocation = getLocation(player.getCurrentLocationId());
        if (currentLocation == null) {
            throw new IllegalStateException("Current location not found for player.");
        }

        List<Choice> filteredChoices = new ArrayList<>();
        for (Choice choice : currentLocation.getAvailableChoices()) {
            if (isChoiceAvailable(choice, player.getFlags())) {
                filteredChoices.add(choice);
            }
        }

        return new GameState(player, currentLocation.getDescription(), filteredChoices);
    }

    private boolean isChoiceAvailable(Choice choice, Map<String, Boolean> playerFlags) {
        if (choice.getCondition() == null || choice.getCondition().isEmpty()) {
            return true;
        }

        String flagName = (String) choice.getCondition().get("flag");
        Boolean requiredValue = (Boolean) choice.getCondition().get("value");

        if (flagName != null && requiredValue != null) {
            return playerFlags.getOrDefault(flagName, false).equals(requiredValue);
        }
        return true;
    }

    public GameState processChoice(String userId, String choiceId) throws ExecutionException, InterruptedException {
        PlayerCharacter player = getPlayerCharacter(userId);
        if (player == null) {
            throw new IllegalArgumentException("Player character not found.");
        }

        Location currentLocation = getLocation(player.getCurrentLocationId());
        if (currentLocation == null) {
            throw new IllegalStateException("Current location not found.");
        }

        Choice chosen = currentLocation.getAvailableChoices().stream()
                .filter(c -> c.getId().equals(choiceId) && isChoiceAvailable(c, player.getFlags()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid or unavailable choice."));

        String newDescription = "";

        switch (chosen.getEffectType()) {
            case "move_location":
                player.setCurrentLocationId(chosen.getTargetId());
                Location newLocation = getLocation(chosen.getTargetId());
                if (newLocation != null) {
                    newDescription = newLocation.getDescription();
                    player.getGameHistory().add(newDescription);
                } else {
                    newDescription = "You moved to an unknown place.";
                }
                break;
            case "find_item":
                Item foundItem = getDocument("items", chosen.getTargetId(), Item.class);
                if (foundItem != null) {
                    player.getInventory().add(foundItem);
                    newDescription = "You found a " + foundItem.getName() + ".";
                    player.getGameHistory().add(newDescription);
                } else {
                    newDescription = "You tried to find an item, but found nothing.";
                }
                break;
            case "start_combat":
                Enemy enemy = getEnemy(chosen.getTargetId());
                if (enemy != null) {
                    newDescription = "You encountered a " + enemy.getName() + "! Combat not fully implemented yet.";
                    player.getGameHistory().add(newDescription);
                } else {
                    newDescription = "You prepared for combat, but no enemy appeared.";
                }
                break;
            case "set_flag":
                if (chosen.getFlagToSet() != null) {
                    player.getFlags().putAll(chosen.getFlagToSet());
                    newDescription = "Something changed in the world...";
                    player.getGameHistory().add(newDescription);
                }
                break;
            case "display_text":
                newDescription = chosen.getTargetId();
                player.getGameHistory().add(newDescription);
                break;
            default:
                newDescription = "Nothing happened.";
                break;
        }

        saveDocument("playerCharacters", userId, player);

        currentLocation = getLocation(player.getCurrentLocationId());
        if (currentLocation == null) {
            throw new IllegalStateException("New current location not found after choice processing.");
        }

        List<Choice> filteredChoices = new ArrayList<>();
        for (Choice choice : currentLocation.getAvailableChoices()) {
            if (isChoiceAvailable(choice, player.getFlags())) {
                filteredChoices.add(choice);
            }
        }

        return new GameState(player, newDescription, filteredChoices);
    }

    @Data
    @NoArgsConstructor
    public static class GameState {
        private PlayerCharacter playerCharacter;
        private String currentNarrative;
        private List<Choice> availableChoices;

        public GameState(PlayerCharacter playerCharacter, String currentNarrative, List<Choice> availableChoices) {
            this.playerCharacter = playerCharacter;
            this.currentNarrative = currentNarrative;
            this.availableChoices = availableChoices;
        }
    }
}

