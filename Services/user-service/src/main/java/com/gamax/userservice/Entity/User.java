package com.gamax.userservice.Entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.GrantedAuthority;

import java.time.LocalDateTime;
import java.util.Collection;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Inheritance(strategy = InheritanceType.JOINED)
public class User implements Serializable, UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;
    private boolean enabled;
    private String resetPasswordToken;
    @Getter
    @Setter
    private String activationToken;
    @Column(nullable = false, unique = true)
    private String username;
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true)
    private String email;
    @Column(name = "last_active_at") // Nouveau champ pour suivre la dernière activité
    private LocalDateTime lastActiveAt ;
    // Ajoutez ces méthodes pour gérer l'URL de l'image de profil
    @Setter
    @Getter
    @Column(name = "profile_picture_url")
    private String profilePictureUrl;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    private int age;
    @Setter
    private String resetToken;
    @Temporal(TemporalType.DATE)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private Date birthday;

    @Temporal(TemporalType.DATE)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @Column(name = "account_creation_date", nullable = false, updatable = false)
    private Date accountCreationDate = new Date();

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(); // À modifier si tu gères les rôles
    }

    @Override
    public String getUsername() {
        return email; // Utilisation de l'email pour l'authentification
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public User setFirstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    public User setLastName(String lastName) {
        this.lastName = lastName;
        return this;
    }

    public User setPassword(String password) {
        this.password = password;
        return this;
    }
    @Column(nullable = false)
    private String role;



    public String getRole() {

        return "";
    }

    public String getRawUsername() {
        return this.username;
    }

    private boolean isBanned;
    private LocalDateTime banEndDate;
    private String banReason;


    public boolean isCurrentlyBanned() {
        return isBanned && (banEndDate == null || banEndDate.isAfter(LocalDateTime.now()));
    }



}