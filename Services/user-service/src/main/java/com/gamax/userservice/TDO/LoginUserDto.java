package com.gamax.userservice.TDO;

import lombok.Getter;

@Getter
public class LoginUserDto {
    private String email;

    private String password;

    public LoginUserDto(String email, String password) {
        this.email = email;
        this.password = password;
    }

    // getters and setters here...
}