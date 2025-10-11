package com.osu.textventures.models;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Enemy {
    private String id;
    private String name;
    private int health;
    private int damage;
    private String description;

    public Enemy(String id, String name, int health, int damage, String description) {
        this.id = id;
        this.name = name;
        this.health = health;
        this.damage = damage;
        this.description = description;
    }
}

