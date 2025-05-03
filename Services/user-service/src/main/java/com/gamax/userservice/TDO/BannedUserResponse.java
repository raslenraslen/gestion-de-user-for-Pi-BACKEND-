package com.gamax.userservice.TDO;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class BannedUserResponse {
    private Long userId;
    private String username;
    private String email;
    private String reason;
    private LocalDateTime banEndDate;
    private String remainingTime;

    public BannedUserResponse(Long userId, String username, String reason,
                              LocalDateTime banEndDate, String email, String remainingTime) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.reason = reason;
        this.banEndDate = banEndDate;
        this.remainingTime = remainingTime;
    }
}