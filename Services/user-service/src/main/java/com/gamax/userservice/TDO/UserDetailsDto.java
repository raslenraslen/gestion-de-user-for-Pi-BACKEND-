package com.gamax.userservice.TDO;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Setter
@Getter
public class UserDetailsDto {
    // Getters et setters
    private Long userId;
    private String firstName;
    private String lastName;
    private String email;
    private String profilePictureUrl;
    private String role;
    private boolean enabled;
    private String Username ;
    private  String imagePath ;
    private int age;
    private Date birthday;
    private Date accountCreationDate;

}
