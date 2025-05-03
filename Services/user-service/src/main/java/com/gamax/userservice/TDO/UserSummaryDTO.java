package com.gamax.userservice.TDO;

import lombok.Data;
import java.util.Date;

@Data
public class UserSummaryDTO {
    private Long userId;
    private String username;
    private String email;
    private String fullName;
    private Date accountCreationDate; // Correspond à ton champ existant
    private String profilePictureUrl; // Optionnel si tu veux l'afficher dans le front
}