package com.gamax.userservice.Entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;


import java.time.LocalDateTime;
@Getter
@Setter
@Entity
public class BanHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private String reason;
    private LocalDateTime banDate;
    private LocalDateTime unbanDate;
    private String bannedBy;
    private String unbanReason;



    // Constructeurs, getters et setters
}