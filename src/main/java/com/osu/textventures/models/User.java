package com.osu.textventures.models;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class User {

    private String id;
    private String username;
    private String passwordHash;
    private String role;

    private PlayerCharacter playerCharacter;

    public User(String id, String username, String passwordHash, String role, PlayerCharacter playerCharacter) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.playerCharacter = playerCharacter;
    }
}
