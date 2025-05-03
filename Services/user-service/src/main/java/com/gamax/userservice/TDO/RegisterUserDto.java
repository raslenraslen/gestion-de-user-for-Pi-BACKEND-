package com.gamax.userservice.TDO;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class RegisterUserDto {
    private String email;
    private String password;
    private String fullName;
    private String username; // Ajout du champ username
    private int age; // Ajout du champ age
    private Date birthday; // Ajout du champ birthday
    private String profilePictureUrl;
}