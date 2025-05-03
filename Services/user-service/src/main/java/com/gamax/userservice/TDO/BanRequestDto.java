package com.gamax.userservice.TDO;
import com.gamax.userservice.enums.BanDuration;
import lombok.Data;

@Data
public class BanRequestDto {
    private BanDuration duration;
    private String reason;
}