package com.osu.textventures.services;

import com.osu.textventures.models.Enemy;
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
        state.setPlayerCurrentHealth(player.getBaseHealth());
        state.setPlayerMaxHealth(player.getBaseHealth());
        state.setPlayerDamage(player.getBaseDamage());
        state.setEnemyCurrentHealth(enemy.getHealth());
        state.setEnemyMaxHealth(enemy.getHealth());
        state.setEnemyDamage(enemy.getDamage());
        state.setEnemyName(enemy.getName());
        state.setEnemyDescription(enemy.getDescription());
        state.setCombatActive(true);
        state.setPlayerTurn(true);
        state.getCombatLog().add("Combat begins! You face " + enemy.getName() + "!");
        state.getCombatLog().add(enemy.getDescription());

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

        switch (action) {
            case ATTACK:
                result = processAttack(state);
                break;
            case DEFEND:
                result = processDefend(state);
                break;
            case FLEE:
                result = processFlee(state);
                break;
            case USE_ITEM:
                result = processUseItem(state);
                break;
            default:
                result.getCombatLog().add("Invalid action!");
                result.setCombatState(state);
                break;
        }

        return result;
    }

    private CombatResult processAttack(CombatState state) {
        CombatResult result = new CombatResult();
        result.setCombatLog(new ArrayList<>());

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
            result.setCombatState(state);
            return result;
        }

        int enemyDamage = calculateDamage(state.getEnemyDamage());
        state.setPlayerCurrentHealth(state.getPlayerCurrentHealth() - enemyDamage);
        result.getCombatLog().add(state.getEnemyName() + " attacks you for " + enemyDamage + " damage!");

        if (state.getPlayerCurrentHealth() <= 0) {
            state.setPlayerCurrentHealth(0);
            state.setCombatActive(false);
            result.getCombatLog().add("You have been defeated...");
            result.setDefeated(true);
        }

        result.setCombatState(state);
        return result;
    }

    private CombatResult processDefend(CombatState state) {
        CombatResult result = new CombatResult();
        result.setCombatLog(new ArrayList<>());

        result.getCombatLog().add("You take a defensive stance!");

        int enemyDamage = calculateDamage(state.getEnemyDamage()) / 2;
        state.setPlayerCurrentHealth(state.getPlayerCurrentHealth() - enemyDamage);
        result.getCombatLog().add(state.getEnemyName() + " attacks, but you block most of the damage! You take " + enemyDamage + " damage.");

        int healAmount = 5;
        state.setPlayerCurrentHealth(Math.min(state.getPlayerCurrentHealth() + healAmount, state.getPlayerMaxHealth()));
        result.getCombatLog().add("You recover " + healAmount + " health!");

        // Check if player is defeated
        if (state.getPlayerCurrentHealth() <= 0) {
            state.setPlayerCurrentHealth(0);
            state.setCombatActive(false);
            result.getCombatLog().add("You have been defeated...");
            result.setDefeated(true);
        }

        result.setCombatState(state);
        return result;
    }

    private CombatResult processFlee(CombatState state) {
        CombatResult result = new CombatResult();
        result.setCombatLog(new ArrayList<>());

        if (random.nextInt(100) < 60) {
            state.setCombatActive(false);
            result.getCombatLog().add("You successfully fled from combat!");
            result.setFled(true);
        } else {
            result.getCombatLog().add("You failed to escape!");

            int enemyDamage = calculateDamage(state.getEnemyDamage());
            state.setPlayerCurrentHealth(state.getPlayerCurrentHealth() - enemyDamage);
            result.getCombatLog().add(state.getEnemyName() + " attacks as you try to flee! You take " + enemyDamage + " damage.");


            if (state.getPlayerCurrentHealth() <= 0) {
                state.setPlayerCurrentHealth(0);
                state.setCombatActive(false);
                result.getCombatLog().add("You have been defeated...");
                result.setDefeated(true);
            }
        }

        result.setCombatState(state);
        return result;
    }

    private CombatResult processUseItem(CombatState state) {
        CombatResult result = new CombatResult();
        result.setCombatLog(new ArrayList<>());

        int healAmount = 20;
        state.setPlayerCurrentHealth(Math.min(state.getPlayerCurrentHealth() + healAmount, state.getPlayerMaxHealth()));
        result.getCombatLog().add("You use a healing item and recover " + healAmount + " health!");

        int enemyDamage = calculateDamage(state.getEnemyDamage());
        state.setPlayerCurrentHealth(state.getPlayerCurrentHealth() - enemyDamage);
        result.getCombatLog().add(state.getEnemyName() + " attacks you for " + enemyDamage + " damage!");

        if (state.getPlayerCurrentHealth() <= 0) {
            state.setPlayerCurrentHealth(0);
            state.setCombatActive(false);
            result.getCombatLog().add("You have been defeated...");
            result.setDefeated(true);
        }

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

        private int playerCurrentHealth;
        private int playerMaxHealth;
        private int playerDamage;

        private int enemyCurrentHealth;
        private int enemyMaxHealth;
        private int enemyDamage;
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