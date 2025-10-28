package com.osu.textventures.models;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Data
@NoArgsConstructor
public class PlayerCharacter {
    private String id;
    private String name;
    private int level;
    private int experience;
    private List<Item> inventory;
    private int baseDamage;
    private int baseHealth;
    private String currentLocationId;
    private List<String> gameHistory;
    private Map<String, Boolean> flags;

    public PlayerCharacter(String id, String name, int level, int experience, List<Item> inventory, int baseDamage, int baseHealth, String currentLocationId, List<String> gameHistory, Map<String, Boolean> flags) {
        this.id = id;
        this.name = name;
        this.level = level;
        this.experience = experience;
        this.inventory = inventory;
        this.baseDamage = baseDamage;
        this.baseHealth = baseHealth;
        this.currentLocationId = currentLocationId;
        this.gameHistory = gameHistory;
        this.flags = flags;
    }

    public PlayerCharacter(String id, String name, int level, int experience, List<Item> inventory, int baseDamage, int baseHealth) {
        this(id, name, level, experience, inventory, baseDamage, baseHealth, "bamboo_forest", new java.util.ArrayList<>(), new HashMap<>());
    }
}

