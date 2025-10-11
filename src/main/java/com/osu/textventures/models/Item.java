package com.osu.textventures.models;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Item {
    private String id;
    private String name;
    private String type;
    private int power;
    private int goldValue;

    public Item(String id, String name, String type, int power, int  goldValue) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.power = power;
        this.goldValue = goldValue;
    }
}
