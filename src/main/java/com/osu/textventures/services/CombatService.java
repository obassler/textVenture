package com.osu.textventures.services;

import com.osu.textventures.models.Enemy;
import com.osu.textventures.models.Item;
import com.osu.textventures.models.PlayerCharacter;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class CombatService {

    private final Random random = new Random();

    public CombatState startCombat(PlayerCharacter player, Enemy enemy) {
        CombatState state = new CombatState();

        int weaponBonus = player.getInventory().stream()
                .filter(item -> "weapon".equalsIgnoreCase(item.getType()))
                .mapToInt(Item::getPower)
                .sum();

        int armorBonus = player.getInventory().stream()
                .filter(item -> "armor".equalsIgnoreCase(item.getType()))
                .mapToInt(Item::getPower)
                .sum();

        state.setPlayerCurrentHealth(player.getCurrentHealth());
        state.setPlayerMaxHealth(player.getBaseHealth() + armorBonus);
        state.setPlayerDamage(player.getBaseDamage() + weaponBonus);
        state.setEnemyCurrentHealth(enemy.getHealth());
        state.setEnemyMaxHealth(enemy.getHealth());
        state.setEnemyDamage(enemy.getDamage());
        state.setEnemyId(enemy.getId());
        state.setEnemyName(enemy.getName());
        state.setEnemyDescription(enemy.getDescription());
        state.setCombatActive(true);
        state.setPlayerTurn(true);
        state.getCombatLog().add("Combat begins! You face " + enemy.getName() + "!");
        state.getCombatLog().add(enemy.getDescription());

        if (weaponBonus > 0) {
            state.getCombatLog().add("Your weapons grant +" + weaponBonus + " damage!");
        }
        if (armorBonus > 0) {
            state.getCombatLog().add("Your armor grants +" + armorBonus + " max health!");
        }

        return state;
    }

    public CombatResult processAction(CombatState state, CombatAction action) {
        CombatResult result = new CombatResult();
        result.setCombatLog(new ArrayList<>());

        if (!state.isCombatActive()) {
            result.getCombatLog().add("Combat is not active!");
            result.setCombatState(state);
            return result;
        }

        CombatResult playerResult;
        boolean usedItem = false;
        switch (action) {
            case ATTACK:
                playerResult = processPlayerAttack(state);
                break;
            case DEFEND:
                playerResult = processPlayerDefend(state);
                break;
            case FLEE:
                playerResult = processPlayerFlee(state);
                break;
            case USE_ITEM:
                playerResult = processPlayerUseItem(state);
                usedItem = true;
                break;
            default:
                result.getCombatLog().add("Invalid action!");
                result.setCombatState(state);
                return result;
        }

        result.getCombatLog().addAll(playerResult.getCombatLog());
        state = playerResult.getCombatState();
        result.setVictory(playerResult.isVictory());
        result.setDefeated(playerResult.isDefeated());
        result.setFled(playerResult.isFled());
        result.setExperienceGained(playerResult.getExperienceGained());

        if (state.isCombatActive() && !result.isFled() && !usedItem) {
            CombatResult enemyResult = processEnemyAttack(state);
            result.getCombatLog().addAll(enemyResult.getCombatLog());
            state = enemyResult.getCombatState();
            result.setDefeated(enemyResult.isDefeated());
        } else if (usedItem && state.isCombatActive()) {
            result.getCombatLog().add("You quickly heal before the enemy can react!");
        }

        result.setCombatState(state);
        return result;
    }

    private CombatResult processPlayerAttack(CombatState state) {
        CombatResult result = new CombatResult();
        result.setCombatLog(new ArrayList<>());

        state.setPlayerDefending(false);

        int playerDamage = calculateDamage(state.getPlayerDamage());
        state.setEnemyCurrentHealth(state.getEnemyCurrentHealth() - playerDamage);
        result.getCombatLog().add("You attack " + state.getEnemyName() + " for " + playerDamage + " damage!");

        if (state.getEnemyCurrentHealth() <= 0) {
            state.setEnemyCurrentHealth(0);
            state.setCombatActive(false);
            result.getCombatLog().add(state.getEnemyName() + " has been defeated!");
            result.setVictory(true);
            result.setExperienceGained(calculateExperience(state.getEnemyMaxHealth(), state.getEnemyDamage()));
            result.getCombatLog().add("You gained " + result.getExperienceGained() + " experience!");
        }

        result.setCombatState(state);
        return result;
    }

    private CombatResult processEnemyAttack(CombatState state) {
        CombatResult result = new CombatResult();
        result.setCombatLog(new ArrayList<>());

        int enemyDamage = calculateDamage(state.getEnemyDamage());

        if (state.isPlayerDefending()) {
            enemyDamage = (int) (enemyDamage * 0.5);
            result.getCombatLog().add(state.getEnemyName() + " attacks! You block and reduce damage!");
            state.setPlayerDefending(false);
        }

        state.setPlayerCurrentHealth(state.getPlayerCurrentHealth() - enemyDamage);
        result.getCombatLog().add(state.getEnemyName() + " deals " + enemyDamage + " damage!");

        if (state.getPlayerCurrentHealth() <= 0) {
            state.setPlayerCurrentHealth(0);
            state.setCombatActive(false);
            result.getCombatLog().add("You have been defeated...");
            result.setDefeated(true);
        }

        result.setCombatState(state);
        return result;
    }

    private CombatResult processPlayerDefend(CombatState state) {
        CombatResult result = new CombatResult();
        result.setCombatLog(new ArrayList<>());

        state.setPlayerDefending(true);
        result.getCombatLog().add("You take a defensive stance!");

        int healAmount = 10;
        int oldHealth = state.getPlayerCurrentHealth();
        int newHealth = Math.min(oldHealth + healAmount, state.getPlayerMaxHealth());
        state.setPlayerCurrentHealth(newHealth);
        int actualHealing = newHealth - oldHealth;
        if (actualHealing > 0) {
            result.getCombatLog().add("You recover " + actualHealing + " health!");
        }

        result.setCombatState(state);
        return result;
    }

    private CombatResult processPlayerFlee(CombatState state) {
        CombatResult result = new CombatResult();
        result.setCombatLog(new ArrayList<>());

        if (random.nextInt(100) < 60) {
            state.setCombatActive(false);
            result.getCombatLog().add("You successfully fled from combat!");
            result.setFled(true);
        } else {
            result.getCombatLog().add("You failed to escape!");
        }

        result.setCombatState(state);
        return result;
    }

    private CombatResult processPlayerUseItem(CombatState state) {
        CombatResult result = new CombatResult();
        result.setCombatLog(new ArrayList<>());

        result.setCombatState(state);
        return result;
    }

    private int calculateDamage(int baseDamage) {
        int variance = (int) (baseDamage * 0.2);
        return baseDamage - variance + random.nextInt(variance * 2 + 1);
    }

    private int calculateExperience(int enemyHealth, int enemyDamage) {
        return (enemyHealth + enemyDamage * 2) / 2;
    }

    public enum CombatAction {
        ATTACK,
        DEFEND,
        FLEE,
        USE_ITEM
    }

    @Data
    @NoArgsConstructor
    public static class CombatState {
        private boolean combatActive;
        private boolean playerTurn;
        private boolean playerDefending;

        private int playerCurrentHealth;
        private int playerMaxHealth;
        private int playerDamage;

        private int enemyCurrentHealth;
        private int enemyMaxHealth;
        private int enemyDamage;
        private String enemyId;
        private String enemyName;
        private String enemyDescription;

        private List<String> combatLog = new ArrayList<>();
    }

    @Data
    @NoArgsConstructor
    public static class CombatResult {
        private CombatState combatState;
        private List<String> combatLog;
        private boolean victory;
        private boolean defeated;
        private boolean fled;
        private int experienceGained;
    }
}