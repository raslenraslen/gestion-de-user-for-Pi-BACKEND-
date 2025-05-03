package com.gamax.userservice.TDO;



import lombok.Data;
import java.time.LocalDateTime;

@Data
public class BanResult {
    private boolean success;
    private String message;
    private Long userId;
    private String duration;
    private String reason;
    private LocalDateTime banEndDate;
    private boolean emailSent ;

    // Constructeur pour succès
    public BanResult(Long userId, String duration, String reason, LocalDateTime banEndDate) {
        this.success = true;
        this.message = "Ban réussi";
        this.userId = userId;
        this.duration = duration;
        this.reason = reason;
        this.banEndDate = banEndDate;
    }

    // Constructeur pour échec
    public BanResult(Long userId) {
        this.success = false;
        this.message = "Utilisateur non trouvé";
        this.userId = userId;
        this.duration = null;
        this.reason = null;
        this.banEndDate = null;
    }
}