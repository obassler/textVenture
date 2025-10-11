package com.osu.textventures.models;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
public class Choice {
    private String id;
    private String text;
    private String effectType;
    private String targetId;
    private Map<String, Boolean> flagToSet;
    private Map<String, Object> condition;

    public Choice(String id, String text, String effectType, String targetId, Map<String, Boolean> flagToSet, Map<String, Object> condition) {
        this.id = id;
        this.text = text;
        this.effectType = effectType;
        this.targetId = targetId;
        this.flagToSet = flagToSet;
        this.condition = condition;
    }
}

