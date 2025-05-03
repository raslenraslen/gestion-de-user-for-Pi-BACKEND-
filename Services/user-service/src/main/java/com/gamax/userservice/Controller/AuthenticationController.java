package com.gamax.userservice.Controller;

import com.gamax.userservice.Entity.User;
import com.gamax.userservice.Service.AuthenticationService;
import com.gamax.userservice.Service.JwtService;
import com.gamax.userservice.TDO.LoginResponse;
import com.gamax.userservice.TDO.LoginUserDto;
import com.gamax.userservice.TDO.RegisterUserDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.naming.AuthenticationException;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {
    private final JwtService jwtService;
    private final AuthenticationService authenticationService;
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationController.class);
    public AuthenticationController(JwtService jwtService, AuthenticationService authenticationService) {
        this.jwtService = jwtService;
        this.authenticationService = authenticationService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticate(@RequestBody LoginUserDto loginUserDto) {
        try {
            // 1. Authentification standard
            User authenticatedUser = authenticationService.authenticate(loginUserDto);

            // 2. Nouveau - Vérification du bannissement
            if (authenticatedUser.isBanned()) {
                String banMessage = "Compte banni. Raison: " + authenticatedUser.getBanReason();
                if (authenticatedUser.getBanEndDate() != null) {
                    banMessage += " | Fin du bannissement: " + authenticatedUser.getBanEndDate();
                } else {
                    banMessage += " (Permanent)";
                }
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of(
                                "status", 403,
                                "error", "Forbidden",
                                "message", banMessage,
                                "timestamp", LocalDateTime.now()
                        ));
            }

            // 3. Ancien code inchangé
            String jwtToken = jwtService.generateToken(authenticatedUser);
            LoginResponse loginResponse = new LoginResponse()
                    .setToken(jwtToken)
                    .setExpiresIn(jwtService.getExpirationTime())
                    .setUserId(authenticatedUser.getUserId())
                    .setFirstName(authenticatedUser.getFirstName())
                    .setLastName(authenticatedUser.getLastName())
                    .setEmail(authenticatedUser.getEmail())
                    .setProfileImageUrl(authenticatedUser.getProfilePictureUrl());

            return ResponseEntity.ok(loginResponse);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "status", 401,
                            "error", "Unauthorized",
                            "message", "Identifiants invalides"
                    ));
        }
    }
    @GetMapping("/activate")
    public ResponseEntity<String> activateAccount(@RequestParam("token") String token) {
        User user = authenticationService.findByActivationToken(token);
        user.setEnabled(true);
        user.setActivationToken(null);
        authenticationService.save(user);
        return ResponseEntity.ok("Account activated successfully");
    }
    @PostMapping("/signup")
    public ResponseEntity<User> register(@RequestBody RegisterUserDto registerUserDto) {
        User registeredUser = authenticationService.signup(registerUserDto);
        return ResponseEntity.ok(registeredUser);
    }


    @PostMapping("/request-reset-password")
    public ResponseEntity<String> requestPasswordReset(@RequestParam("email") String email) {
        authenticationService.requestPasswordReset(email);
        return ResponseEntity.ok("{\"message\": \"Password reset email sent\"}");
    }

    @PostMapping("/validate-reset-email")
    public ResponseEntity<String> validateResetEmail(@RequestParam("email") String email) {
        boolean isValid = authenticationService.validatePasswordResetEmail(email);
        if (isValid) {
            return ResponseEntity.ok("{\"message\": \"Valid email\"}");
        } else {
            return ResponseEntity.badRequest().body("{\"message\": \"Invalid or expired email\"}");
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestParam("email") String email, @RequestParam("newPassword") String newPassword) {
        try {
            authenticationService.resetPasswordByEmail(email, newPassword);
            return ResponseEntity.ok("Password has been reset successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    @PostMapping("/request-otp")
    public ResponseEntity<String> requestOtp(@RequestParam("email") String email) {
        authenticationService.sendOtpEmail(email);
        return ResponseEntity.ok("OTP sent to your email.");
    }

    @PostMapping("/validate-otp")
    public ResponseEntity<String> validateOtp(@RequestParam("email") String email, @RequestParam("otp") String otp) {
        boolean isValid = authenticationService.validateOtp(email, otp);
        if (isValid) {
            return ResponseEntity.ok("OTP validated successfully. Access granted.");
        } else {
            return ResponseEntity.status(401).body("Invalid OTP. Access denied.");
        }
    }
    @GetMapping("/check-username") // Endpoint qui prend le nom en paramètre d'URL
    // Le type de retour sera un objet JSON simple indiquant la disponibilité
    public ResponseEntity<?> checkUsernameAvailability(@RequestParam("username") String username) {
        logger.info("Requête reçue pour vérifier la disponibilité du nom d'utilisateur: '{}'", username);

        if (username == null || username.trim().isEmpty()) {
            logger.warn("Nom d'utilisateur manquant ou vide pour la vérification.");
            // Retourner un statut 400 Bad Request avec un message clair
            return ResponseEntity.badRequest().body(Map.of(
                    "available", false, // Indique que la vérification n'est pas valide
                    "message", "Le nom d'utilisateur est requis pour la vérification."
            ));
        }

        try {
            // Appeler la méthode du service qui retourne true si disponible, false sinon
            boolean isAvailable = authenticationService.checkUsername(username);

            if (isAvailable) {
                logger.info("Nom d'utilisateur '{}' est disponible.", username);
                // Retourner 200 OK avec un indicateur "available: true"
                return ResponseEntity.ok(Map.of(
                        "available", true,
                        "message", "Ce nom d'utilisateur est disponible !"
                ));
            } else {
                logger.info("Nom d'utilisateur '{}' est déjà pris.", username);
                // Retourner 200 OK avec un indicateur "available: false" et un message
                return ResponseEntity.ok(Map.of(
                        "available", false,
                        "message", "Ce nom d'utilisateur est déjà pris."
                ));
            }
        } catch (Exception e) {
            logger.error("Erreur lors de la vérification de la disponibilité du nom d'utilisateur '{}'", username, e);
            // En cas d'erreur interne (ex: DB inaccessible), retourner un 500
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "available", false, // La disponibilité n'a pas pu être confirmée
                    "message", "Une erreur est survenue lors de la vérification de la disponibilité."
            ));
        }
    }

    // --- NOUVEAU : Endpoint pour vérifier la disponibilité de l'email ---
    @GetMapping("/check-email")
    public ResponseEntity<?> checkEmailAvailability(@RequestParam("email") String email) {
        logger.info("Requête reçue pour vérifier la disponibilité de l'email: '{}'", email);

        if (email == null || email.trim().isEmpty()) {
            logger.warn("Email manquant ou vide pour la vérification.");
            return ResponseEntity.badRequest().body(Map.of(
                    "available", false,
                    "message", "L'email est requis pour la vérification."
            ));
        }

        try {
            boolean isAvailable = authenticationService.checkEmail(email);
            if (isAvailable) {
                logger.info("Email '{}' est disponible.", email);
                return ResponseEntity.ok(Map.of(
                        "available", true,
                        "message", "Cet email est disponible !"
                ));
            } else {
                logger.info("Email '{}' est déjà utilisé.", email);
                return ResponseEntity.ok(Map.of(
                        "available", false,
                        "message", "Cet email est déjà utilisé."
                ));
            }
        } catch (Exception e) {
            logger.error("Erreur lors de la vérification de la disponibilité de l'email '{}'", email, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "available", false,
                    "message", "Une erreur est survenue lors de la vérification de la disponibilité de l'email."
            ));
        }
    }


}