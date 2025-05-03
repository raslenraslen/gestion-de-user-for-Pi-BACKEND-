package com.gamax.userservice.TDO;

import lombok.Getter;

public class LoginResponse {
    @Getter
    private String token;
    @Getter
    private long expiresIn;
    @Getter
    private long userId;
    @Getter
    private String firstName;
    @Getter
    private String lastName;
    @Getter
    private String email;
    @Getter
    private String profileImageUrl; // Ajoutez cette ligne
    private  String username;
    // Getters et setters pour tous les champs

    public LoginResponse setToken(String token) {
        this.token = token;
        return this;
    }

    public LoginResponse setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
        return this;
    }

    public LoginResponse setUserId(long userId) {
        this.userId = userId;
        return this;
    }

    public LoginResponse setFirstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    public LoginResponse setLastName(String lastName) {
        this.lastName = lastName;
        return this;
    }

    public LoginResponse setEmail(String email) {
        this.email = email;
        return this;
    }

    public LoginResponse setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
        return this;
    }
}