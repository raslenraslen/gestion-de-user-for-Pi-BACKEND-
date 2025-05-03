package com.gamax.userservice.TDO;

public class UsernameOnlyDto {
    private String username;

    // Constructeur
    public UsernameOnlyDto(String username) {
        this.username = username;
    }

    // Getter
    public String getUsername() {
        return username;
    }
}