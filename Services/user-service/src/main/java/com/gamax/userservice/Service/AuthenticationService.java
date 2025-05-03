package com.gamax.userservice.Service;

import com.gamax.userservice.Entity.User;
import com.gamax.userservice.Repository.UserRepository;
import com.gamax.userservice.TDO.LoginUserDto;
import com.gamax.userservice.TDO.RegisterUserDto;
import com.gamax.userservice.util.OtpUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Import if needed
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@Service
public class AuthenticationService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    @Autowired
    private EmailService emailService;
    private JavaMailSender mailSender;
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);

    public AuthenticationService(
            UserRepository userRepository,
            AuthenticationManager authenticationManager,
            PasswordEncoder passwordEncoder, JavaMailSender mailSender
    ) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailSender = mailSender;
    }

    public User signup(RegisterUserDto input) {
        if (input.getFullName() == null || input.getFullName().trim().isEmpty()) {
            throw new IllegalArgumentException("Full name cannot be empty");
        }

        String[] nameParts = input.getFullName().trim().split(" ", 2);
        String firstName = nameParts[0];
        String lastName = nameParts.length > 1 ? nameParts[1] : "";

        User user = new User();
        user.setFirstName(firstName);
        user.setUsername(input.getUsername());
        user.setAge(input.getAge());
        user.setBirthday(input.getBirthday());
        user.setAccountCreationDate(new Date());
        user.setLastName(lastName);
        user.setEmail(input.getEmail());
        user.setPassword(passwordEncoder.encode(input.getPassword()));
        user.setActivationToken(UUID.randomUUID().toString());
        user.setRole("USER");

        user.setProfilePictureUrl(input.getProfilePictureUrl());
        User savedUser = userRepository.save(user);
        String subject = "Account Activation";
        String confirmationUrl = "http://gamaxdns.duckdns.org:8080/api/auth/activate?token=" + user.getActivationToken();
        String message = "Please click the following link to activate your account: " + confirmationUrl;
        emailService.sendEmail(user.getEmail(), subject, message);

        return savedUser;
    }

    public User authenticate(LoginUserDto input) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        input.getEmail(),
                        input.getPassword()
                )
        );

        return userRepository.findByEmail(input.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public User findByActivationToken(String token) {
        return userRepository.findByActivationToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid activation token"));
    }

    public void save(User user) {
        userRepository.save(user);
    }

    public void requestPasswordReset(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setResetToken(UUID.randomUUID().toString());
        userRepository.save(user);

        String subject = "Password Reset Request";
        String resetUrl = "http://gamaxdns.duckdns.org:8080/api/auth/reset-password?email=" + user.getEmail();
        String message = "Please click the following link to reset your password: " + resetUrl;
        emailService.sendEmail(user.getEmail(), subject, message);
    }

    public boolean validatePasswordResetEmail(String email) {
        Optional<User> userOptional = userRepository.findByEmail(email);
        return userOptional.isPresent();
    }

    public void resetPasswordByEmail(String email, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        userRepository.save(user);
    }

    @Autowired
    private OtpUtil otpUtil;

    private Map<String, String> otpStorage = new HashMap<>();

    public void sendOtpEmail(String email) {
        String otp = otpUtil.generateOtp();
        otpStorage.put(email, otp);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Your OTP Code");
        message.setText("Your OTP code is: " + otp);
        mailSender.send(message);
    }

    public boolean validateOtp(String email, String otp) {
        String storedOtp = otpStorage.get(email);
        return storedOtp != null && storedOtp.equals(otp);
    }

    public boolean checkUsername(String username) {
        logger.debug("Vérification de la disponibilité du nom d'utilisateur: '{}'", username);
        if (username == null || username.trim().isEmpty()) {
            logger.warn("Nom d'utilisateur nul ou vide pour vérification de disponibilité.");
            return false; // Un nom vide n'est pas considéré comme disponible
        }
        // Utilise findByUsername et retourne true si Optional est vide (pas trouvé = disponible)
        Optional<User> userOptional = userRepository.findByUsername(username);
        boolean isAvailable = userOptional.isEmpty();
        logger.debug("Nom d'utilisateur '{}' est disponible: {}", username, isAvailable);
        return isAvailable;
    }

    // --- NOUVEAU : Méthode pour vérifier la disponibilité de l'email (retourne boolean) ---
    public boolean checkEmail(String email) {
        logger.debug("Vérification de la disponibilité de l'email: '{}'", email);
        if (email == null || email.trim().isEmpty()) {
            logger.warn("Email nul ou vide pour vérification de disponibilité.");
            return false; // Un email vide n'est pas considéré comme disponible
        }
        // Utilise findByEmail et retourne true si Optional est vide (pas trouvé = disponible)
        Optional<User> userOptional = userRepository.findByEmail(email);
        boolean isAvailable = userOptional.isEmpty();
        logger.debug("Email '{}' est disponible: {}", email, isAvailable);
        return isAvailable;
    }

}