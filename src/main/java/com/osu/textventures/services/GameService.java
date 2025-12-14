package com.osu.textventures.services;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import com.osu.textventures.models.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import com.osu.textventures.models.GameState;


@Service
public class GameService {

    private static final Logger logger = LoggerFactory.getLogger(GameService.class);

    private final Firestore db = FirestoreClient.getFirestore();
    private final CombatService combatService;

    private final Map<String, CombatService.CombatState> activeCombats = new HashMap<>();

    public GameService(CombatService combatService) {
        this.combatService = combatService;
    }

    private int getExperienceForLevel(int level) {
        int totalXp = 0;
        for (int i = 2; i <= level; i++) {
            totalXp += (i - 1) * 10;
        }
        return totalXp;
    }

    private void checkAndProcessLevelUp(PlayerCharacter player) {
        while (true) {
            int nextLevel = player.getLevel() + 1;
            int xpRequired = getExperienceForLevel(nextLevel);

            if (player.getExperience() >= xpRequired) {
                player.setLevel(nextLevel);

                int healthIncrease = 20;
                int damageIncrease = 3;

                player.setBaseHealth(player.getBaseHealth() + healthIncrease);
                player.setCurrentHealth(player.getCurrentHealth() + healthIncrease); // Heal on level up
                player.setBaseDamage(player.getBaseDamage() + damageIncrease);

                player.getGameHistory().add("Level up! You are now level " + nextLevel +
                    ". Health +" + healthIncrease + ", Damage +" + damageIncrease);
            } else {
                break;
            }
        }
    }

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

    private <T> void saveDocument(String documentId, T data) throws ExecutionException, InterruptedException {
        db.collection("playerCharacters").document(documentId).set(data).get();
    }

    public PlayerCharacter getPlayerCharacter(String userId) throws ExecutionException, InterruptedException {
        PlayerCharacter player = getDocument("playerCharacters", userId, PlayerCharacter.class);

        if (player != null && player.getCurrentHealth() == 0) {
            player.setCurrentHealth(player.getBaseHealth());
            saveDocument(userId, player);
        }

        return player;
    }

    public Location getLocation(String locationId) throws ExecutionException, InterruptedException {
        return getDocument("locations", locationId, Location.class);
    }

    public Enemy getEnemy(String enemyId) throws ExecutionException, InterruptedException {
        return getDocument("enemies", enemyId, Enemy.class);
    }

    public GameState startGame(String userId, String characterName) throws ExecutionException, InterruptedException {
        logger.info("Starting new game for user: {} with character name: {}", userId, characterName);

        PlayerCharacter existingCharacter = getPlayerCharacter(userId);
        if (existingCharacter != null) {
            logger.warn("User {} already has an existing character", userId);
            throw new IllegalArgumentException("Player character already exists for this user.");
        }

        PlayerCharacter newCharacter = new PlayerCharacter(
                userId, characterName, 1, 0, new ArrayList<>(), 10, 100
        );

        saveDocument(userId, newCharacter);
        logger.debug("Created new character for user: {}", userId);

        Location startLocation = getLocation(newCharacter.getCurrentLocationId());
        if (startLocation == null) {
            logger.error("Starting location not found for user: {}", userId);
            throw new IllegalStateException("Starting location not found.");
        }

        newCharacter.getGameHistory().add(startLocation.getDescription());
        saveDocument(userId, newCharacter);

        logger.info("Game started successfully for user: {}", userId);
        return new GameState(newCharacter, startLocation.getDescription(), startLocation.getAvailableChoices());
    }
    public void resetGame(String userId) throws ExecutionException, InterruptedException {
        logger.info("Resetting game for user: {}", userId);
        db.collection("playerCharacters").document(userId).delete().get();
        logger.info("Game reset completed for user: {}", userId);
    }

    public GameState getGameState(String userId) throws ExecutionException, InterruptedException {
        logger.debug("Fetching game state for user: {}", userId);

        PlayerCharacter player = getPlayerCharacter(userId);
        if (player == null) {
            logger.warn("No player character found for user: {}", userId);
            throw new IllegalArgumentException("Player character not found. Please start a new game.");
        }

        Location currentLocation = getLocation(player.getCurrentLocationId());
        if (currentLocation == null) {
            logger.error("Location {} not found for user: {}", player.getCurrentLocationId(), userId);
            throw new IllegalStateException("Current location not found for player.");
        }

        List<Choice> filteredChoices = new ArrayList<>();
        for (Choice choice : currentLocation.getAvailableChoices()) {
            if (isChoiceAvailable(choice, player.getFlags())) {
                filteredChoices.add(choice);
            }
        }

        GameState gameState = new GameState(player, currentLocation.getDescription(), filteredChoices);

        if (player.getFlags().getOrDefault("defeated_ancient_dragon", false)) {
            gameState.setGameCompleted(true);
            logger.info("Game completed for user: {}", userId);
        }

        logger.debug("Game state fetched successfully for user: {}", userId);
        return gameState;
    }

    private boolean isChoiceAvailable(Choice choice, Map<String, Boolean> playerFlags) {
        if (choice.getCondition() == null || choice.getCondition().isEmpty()) {
            return true;
        }

        if (choice.getCondition().containsKey("flag") && choice.getCondition().containsKey("value")) {
            String flagName = (String) choice.getCondition().get("flag");
            Boolean requiredValue = (Boolean) choice.getCondition().get("value");
            if (flagName != null && requiredValue != null) {
                return playerFlags.getOrDefault(flagName, false).equals(requiredValue);
            }
        }

        for (Map.Entry<String, Object> entry : choice.getCondition().entrySet()) {
            String flagName = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof Boolean requiredValue) {
                Boolean playerValue = playerFlags.getOrDefault(flagName, false);
                if (!playerValue.equals(requiredValue)) {
                    return false;
                }
            }
        }

        return true;
    }

    public GameState processCombatAction(String userId, CombatService.CombatAction action)
            throws ExecutionException, InterruptedException {

        logger.info("Processing combat action {} for user: {}", action, userId);

        PlayerCharacter player = getPlayerCharacter(userId);
        if (player == null) {
            logger.warn("Player not found for combat action, user: {}", userId);
            throw new IllegalArgumentException("Player not found.");
        }

        CombatService.CombatState combatState = activeCombats.get(userId);
        if (combatState == null) {
            logger.warn("No active combat found for user: {}", userId);
            throw new IllegalArgumentException("No active combat for this user.");
        }

        logger.debug("Combat state: player HP={}, enemy HP={}", combatState.getPlayerCurrentHealth(), combatState.getEnemyCurrentHealth());

        if (action == CombatService.CombatAction.USE_ITEM) {
            Item healingItem = player.getInventory().stream()
                    .filter(item -> "healing".equalsIgnoreCase(item.getType()) || "food".equalsIgnoreCase(item.getType()))
                    .findFirst()
                    .orElse(null);

            if (healingItem == null) {
                StringBuilder itemsMessage = new StringBuilder("You have no healing items to use! ");
                if (player.getInventory().isEmpty()) {
                    itemsMessage.append("Your inventory is empty.");
                } else {
                    itemsMessage.append("You have: ");
                    for (Item item : player.getInventory()) {
                        itemsMessage.append(item.getName()).append(" (").append(item.getType()).append("), ");
                    }
                    itemsMessage.setLength(itemsMessage.length() - 2);
                    itemsMessage.append(". Weapons and armor are automatically equipped!");
                }

                combatState.getCombatLog().add(itemsMessage.toString());

                GameState gameState = new GameState();
                gameState.setPlayerCharacter(player);
                gameState.setCombatState(combatState);
                gameState.setCurrentNarrative(itemsMessage.toString());
                gameState.setAvailableChoices(List.of());
                return gameState;
            }

            int healAmount = healingItem.getPower();
            int oldHealth = combatState.getPlayerCurrentHealth();
            int newHealth = Math.min(oldHealth + healAmount, combatState.getPlayerMaxHealth());
            combatState.setPlayerCurrentHealth(newHealth);
            combatState.getCombatLog().add("You use " + healingItem.getName() + " and recover " + (newHealth - oldHealth) + " health!");

            player.getInventory().remove(healingItem);
            player.setCurrentHealth(newHealth);
            saveDocument(userId, player);
        }

        CombatService.CombatResult result = combatService.processAction(combatState, action);
        combatState = result.getCombatState();

        player.setCurrentHealth(combatState.getPlayerCurrentHealth());

        if (!combatState.isCombatActive()) {
            activeCombats.remove(userId);
            if (result.isVictory()) {
                int oldLevel = player.getLevel();
                player.setExperience(player.getExperience() + result.getExperienceGained());
                checkAndProcessLevelUp(player);

                String defeatFlag = "defeated_" + combatState.getEnemyId();
                player.getFlags().put(defeatFlag, true);

                if ("grumpy_fisherman".equals(combatState.getEnemyId())) {
                    player.getFlags().put("fisherman_distracted", true);
                }

                saveDocument(userId, player);

                if (oldLevel < player.getLevel()) {
                    result.getCombatLog().add("Level up! You are now level " + player.getLevel() + "!");
                }

                return getGameState(userId);
            } else if (result.isDefeated()) {
                saveDocument(userId, player);
                return getGameState(userId);
            } else if (result.isFled()) {
                saveDocument(userId, player);
                return getGameState(userId);
            }
        } else {
            activeCombats.put(userId, combatState);
            saveDocument(userId, player);
        }

        GameState gameState = new GameState();
        gameState.setPlayerCharacter(player);
        gameState.setCombatState(combatState);
        gameState.setCurrentNarrative(String.join("\n", result.getCombatLog()));
        gameState.setAvailableChoices(List.of());

        return gameState;
    }


    public GameState processChoice(String userId, String choiceId) throws ExecutionException, InterruptedException {
        logger.info("Processing choice {} for user: {}", choiceId, userId);

        PlayerCharacter player = getPlayerCharacter(userId);
        if (player == null) {
            logger.warn("Player not found for user: {}", userId);
            throw new IllegalArgumentException("Player character not found.");
        }

        Location currentLocation = getLocation(player.getCurrentLocationId());
        if (currentLocation == null) {
            logger.error("Current location not found for user: {}", userId);
            throw new IllegalStateException("Current location not found.");
        }

        Choice chosen = currentLocation.getAvailableChoices().stream()
                .filter(c -> c.getId().equals(choiceId) && isChoiceAvailable(c, player.getFlags()))
                .findFirst()
                .orElseThrow(() -> {
                    logger.warn("Invalid choice {} for user: {}", choiceId, userId);
                    return new IllegalArgumentException("Invalid or unavailable choice.");
                });

        logger.debug("User {} selected choice with effect type: {}", userId, chosen.getEffectType());

        String newDescription = "";

        switch (chosen.getEffectType()) {
            case "move_location":
                player.setCurrentLocationId(chosen.getTargetId());
                Location newLocation = getLocation(chosen.getTargetId());
                if (newLocation != null) {
                    newDescription = newLocation.getDescription();
                    player.getGameHistory().add(newDescription);

                    // Set flags if specified
                    if (chosen.getFlagToSet() != null) {
                        player.getFlags().putAll(chosen.getFlagToSet());
                    }
                } else {
                    newDescription = "You moved to an unknown place.";
                }
                break;
            case "find_item":
                Item foundItem = getDocument("items", chosen.getTargetId(), Item.class);
                if (foundItem != null) {
                    player.getInventory().add(foundItem);
                    newDescription = "You found a " + foundItem.getName() + "!";
                    player.getGameHistory().add(newDescription);

                    if (chosen.getFlagToSet() != null) {
                        player.getFlags().putAll(chosen.getFlagToSet());
                    }
                } else {
                    newDescription = "You tried to find an item, but found nothing.";
                }
                break;
            case "start_combat":
                Enemy enemy = getEnemy(chosen.getTargetId());
                if (enemy != null) {
                    CombatService.CombatState combatState = combatService.startCombat(player, enemy);

                    activeCombats.put(userId, combatState);

                    newDescription = "You are now in combat with " + enemy.getName() + "!";
                    player.getGameHistory().add(newDescription);

                    saveDocument(userId, player);

                    GameState gameState = new GameState(player, newDescription, new ArrayList<>());
                    gameState.setCombatState(combatState);
                    return gameState;
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

                if (chosen.getFlagToSet() != null) {
                    player.getFlags().putAll(chosen.getFlagToSet());
                }
                break;
            default:
                newDescription = "Nothing happened.";
                break;
        }

        saveDocument(userId, player);

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


}

