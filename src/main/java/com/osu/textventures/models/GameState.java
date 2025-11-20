package com.osu.textventures.models;

import com.osu.textventures.services.CombatService;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@Data
@NoArgsConstructor
public class GameState {
    private PlayerCharacter playerCharacter;
    private String currentNarrative;
    private List<Choice> availableChoices;
    private CombatService.CombatState combatState;
    private boolean gameCompleted;

    public GameState(PlayerCharacter playerCharacter, String currentNarrative, List<Choice> availableChoices) {
        this.playerCharacter = playerCharacter;
        this.currentNarrative = currentNarrative;
        this.availableChoices = availableChoices;
        this.gameCompleted = false;
    }
}
