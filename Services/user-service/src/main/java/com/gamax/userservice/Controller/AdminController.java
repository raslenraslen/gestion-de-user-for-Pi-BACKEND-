package com.gamax.userservice.Controller;

import com.gamax.userservice.Entity.BanHistory;
import com.gamax.userservice.Entity.User;
import com.gamax.userservice.Repository.BanHistoryRepository;
import com.gamax.userservice.Service.UserService;
import com.gamax.userservice.TDO.BannedUserResponse;
import com.gamax.userservice.enums.BanDuration;
import com.gamax.userservice.Repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final BanHistoryRepository banHistoryRepository;

    public AdminController(UserRepository userRepository, BanHistoryRepository banHistoryRepository, UserService userService) {
        this.userRepository = userRepository;
        this.banHistoryRepository = banHistoryRepository;
    }

    @GetMapping("/banned-users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<BannedUserResponse>> getBannedUsers(
            @RequestParam(required = false) BanDuration duration,
            @PageableDefault(size = 10, sort = "banEndDate", direction = Sort.Direction.ASC) Pageable pageable) {

        LocalDateTime maxDate = (duration != null && !duration.equals(BanDuration.PERMANENT))
                ? LocalDateTime.now().plusDays(duration.getDays())
                : null;

        Page<User> bannedUsers = userRepository.findBannedUsersFiltered(duration, maxDate, pageable);

        return ResponseEntity.ok(bannedUsers.map(user ->
                new BannedUserResponse(
                        user.getUserId(),
                        user.getUsername(),
                        user.getBanReason(),
                        user.getBanEndDate(),
                        user.getEmail(),
                        calculateRemainingBanTime(user)
                )
        ));
    }

    private String calculateRemainingBanTime(User user) {
        if (user.getBanEndDate() == null) {
            return "Permanent";
        }

        Duration remaining = Duration.between(LocalDateTime.now(), user.getBanEndDate());
        return String.format("%dj %dh", remaining.toDays(), remaining.toHours() % 24);
    }


    @PostMapping("/unban/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> unbanUser(
            @PathVariable Long userId,
            @RequestParam(required = false) String unbanReason) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur non trouvé"));

        if (!user.isBanned()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cet utilisateur n'est pas banni");
        }

        // 1. Créez l'entrée d'historique AVANT de modifier l'utilisateur
        BanHistory history = new BanHistory();
        history.setUserId(user.getUserId());
        history.setReason(user.getBanReason()); // La raison du ban original
        history.setBanDate(user.getBanEndDate() != null
                ? LocalDateTime.now().minusDays(7) // Date approximative du ban
                : LocalDateTime.now());
        history.setUnbanDate(LocalDateTime.now());
        history.setBannedBy(SecurityContextHolder.getContext().getAuthentication().getName());
        history.setUnbanReason(unbanReason); // Ajoutez ce champ à votre entité BanHistory

        // 2. Sauvegardez l'historique
        banHistoryRepository.save(history);

        // 3. Puis débannissez l'utilisateur
        user.setBanned(false);
        user.setBanReason(null);
        user.setBanEndDate(null);
        userRepository.save(user);

        return ResponseEntity.ok("Utilisateur " + user.getEmail() + " a été débanni. Historique enregistré.");
    }

    @GetMapping("/ban-history/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<BanHistory>> getBanHistory(@PathVariable Long userId) {
        return ResponseEntity.ok(banHistoryRepository.findByUserIdOrderByBanDateDesc(userId));
    }

}