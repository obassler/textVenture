package com.osu.textventures.models;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
public class Location {
    private String id;
    private String name;
    private String description;
    private List<Choice> availableChoices;
    private List<Item> itemsPresent;
    private String enemyPresentId;

    public Location(String id, String name, String description, List<Choice> availableChoices, List<Item> itemsPresent, String enemyPresentId) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.availableChoices = availableChoices;
        this.itemsPresent = itemsPresent;
        this.enemyPresentId = enemyPresentId;
    }
}

