package com.gamax.userservice.util;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class OtpUtil {

    private static final SecureRandom secureRandom = new SecureRandom();

    public String generateOtp() {
        int otp = secureRandom.nextInt(900000) + 100000; // Generate a 6-digit OTP
        return String.valueOf(otp);
    }
}
