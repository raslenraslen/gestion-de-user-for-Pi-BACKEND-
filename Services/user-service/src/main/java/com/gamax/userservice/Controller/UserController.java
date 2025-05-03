package com.gamax.userservice.Controller;

import com.gamax.userservice.Entity.Admin;
import com.gamax.userservice.Entity.Client;
import com.gamax.userservice.Entity.User;
import com.gamax.userservice.Repository.UserRepository;
import com.gamax.userservice.Service.AdminService;
import com.gamax.userservice.Service.ClientService;
import com.gamax.userservice.Service.EmailService;
import com.gamax.userservice.Service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamax.userservice.TDO.*;
import com.gamax.userservice.enums.BanDuration;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.awt.print.Pageable;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {
    @Autowired
    private UserService userService;
    @Autowired
    private AdminService adminService;
    @Autowired
    private ClientService clientService;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<User>> getUsers() {
        return ResponseEntity.ok().body(userService.getAll());
    }

    @GetMapping("TDO/{userId}")
    public ResponseEntity<UserDTO> getUser(@PathVariable Long userId) {
        UserDTO userDTO = userService.getTDObyID(userId);
        return ResponseEntity.ok(userDTO);
    }

    @PostMapping("TDO/by-ids")
    public ResponseEntity<List<UserDTO>> getUsersByIds(@RequestBody List<Long> userIds) {
        List<UserDTO> userDTOs = userService.getTDDOUsersByIds(userIds);
        return ResponseEntity.ok(userDTOs);
    }

    @GetMapping("/{type}")
    public List<?> getAll(@PathVariable String type) {
        return switch (type.toLowerCase()) {
            case "user" -> userService.getAll();
            case "admin" -> adminService.getAll();
            case "client" -> clientService.getAll();
            default -> throw new IllegalArgumentException("Invalid entity type: " + type);
        };
    }

    @PostMapping("/{type}/add")
    public ResponseEntity<?> addUser(@PathVariable String type, @RequestBody String object) {
        try {//TODO: Upload image de profile
            switch (type.toLowerCase()) {
                case "user":
                    User user = objectMapper.readValue(object, User.class);
                    return ResponseEntity.status(201).body(userService.save(user));
                case "admin":
                    Admin admin = objectMapper.readValue(object, Admin.class);
                    return ResponseEntity.status(201).body(adminService.save(admin));
                case "client":
                    Client client = objectMapper.readValue(object, Client.class);
                    return ResponseEntity.status(201).body(clientService.save(client));
                default:
                    throw new IllegalArgumentException("Invalid user type: " + type);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize JSON payload: " + e.getMessage(), e);
        }
    }

    @PutMapping("/{type}/update")
    public ResponseEntity<?> updater_user(@RequestBody String object, @PathVariable String type) {
        try {
            switch (type.toLowerCase()) {
                case "user":
                    User user = objectMapper.readValue(object, User.class);
                    return ResponseEntity.status(201).body(userService.update(user.getUserId(), user));
                case "client":
                    Client client = objectMapper.readValue(object, Client.class);
                    return ResponseEntity.status(201).body(clientService.update(client.getUserId(), client));
                case "admin":
                    Admin admin = objectMapper.readValue(object, Admin.class);
                    return ResponseEntity.status(201).body(adminService.update(admin.getUserId(), admin));
                default:
                    throw new IllegalArgumentException("Invalid user type: " + type);
            }
        } catch (RuntimeException e) {
            throw new RuntimeException(e.getMessage(), e);

            //    return ResponseEntity.status(404).body("Entity not Found");
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize JSON payload: " + e.getMessage(), e);
        }
    }

    @DeleteMapping("/delete")
    public ResponseEntity deleteUser(@RequestBody User user) {
        return ResponseEntity.ok().body(userService.delete(user.getUserId()));
    }


    @GetMapping("/me")
    public ResponseEntity<User> authenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        User currentUser = (User) authentication.getPrincipal();

        return ResponseEntity.ok(currentUser);
    }

    @GetMapping("/")
    public ResponseEntity<List<User>> allUsers() {
        List<User> users = userService.allUsers();

        return ResponseEntity.ok(users);
    }

    /*
    @GetMapping("/byEmail")
    public ResponseEntity<UserDetailsDto> getUserDetailsByEmail(@RequestParam String email) {
        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isPresent()) {
            User user = userOptional.get();

            UserDetailsDto dto = new UserDetailsDto();
            dto.setEmail(user.getUsername());
            dto.setUserId(user.getUserId());
            dto.setFirstName(user.getFirstName());
            dto.setLastName(user.getLastName());
            dto.setEmail(user.getEmail());
            dto.setRole(user.getRole());
            dto.setEnabled(user.isEnabled());
            dto.setUsername(user.getRawUsername());


            // ✅ Construire le nom de fichier basé sur l'email
            String fileName = email.split("@")[0] + ".jpg";  // Exemple : raslenm39@gmail.com -> raslenm39.jpg
            Path imagePath = Paths.get("C:/Users/user/Desktop/Pi dev/gamemax/Services/user-service/uploads", fileName);

            // Log pour vérifier le chemin du fichier
            System.out.println("Chemin de l'image : " + imagePath);

            try {
                // 👉 Vérifier si le fichier existe
                if (Files.exists(imagePath)) {
                    // Log pour confirmer que le fichier existe
                    System.out.println("Fichier trouvé : " + imagePath);

                    byte[] imageBytes = Files.readAllBytes(imagePath);

                    // 👉 Encoder l'image en Base64 pour l'afficher dans la réponse
                    String base64Image = "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(imageBytes);
                    dto.setProfilePictureUrl(base64Image);  // Ajouter l'image encodée à la réponse
                } else {

                    System.out.println("Fichier non trouvé : " + imagePath);
                    dto.setProfilePictureUrl(null);
                }
            } catch (IOException e) {
                e.printStackTrace();
                dto.setProfilePictureUrl(null); // ou une image par défaut encodée si tu veux
            }

            return ResponseEntity.ok(dto);
        } else {
            return ResponseEntity.status(404).body(null);  // Si l'utilisateur n'est pas trouvé
        }
    }

    @GetMapping("/uploads")
    public ResponseEntity<Resource> getImageByEmail(@RequestParam String email) {
        try {
            // Vérification simple de l'email pour s'assurer qu'il est bien formé
            if (email == null || !email.contains("@")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(null); // Ou un message d'erreur plus détaillé si nécessaire
            }

            // Retirer le domaine de l'email (par exemple, 'raslenm39@gmail.com' devient 'raslenm39')
            String fileName = email.split("@")[0] + ".jpg";

            // Chemin vers le dossier où les images sont stockées
            Path filePath = Paths.get("C:/Users/user/Desktop/Pi dev/gamemax/Services/user-service/uploads").resolve(fileName);

            // Vérifie si le fichier existe
            if (Files.exists(filePath)) {
                // Si le fichier existe, renvoie l'image
                Resource resource = new UrlResource(filePath.toUri());
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                        .body(resource);
            } else {
                // Si le fichier n'existe pas, renvoie une erreur 404
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(null); // Ou un message d'erreur plus détaillé
            }
        } catch (MalformedURLException e) {
            // Si le chemin est incorrect, renvoie une erreur 500
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null); // Ou un message d'erreur plus détaillé
        }
    }


    */
    @GetMapping("/byId")
    public ResponseEntity<UserDetailsDto> getUserDetailsById(@RequestParam Long id) {
        Optional<User> userOptional = userRepository.findById(id);

        if (userOptional.isPresent()) {
            User user = userOptional.get();

            UserDetailsDto dto = new UserDetailsDto();
            dto.setEmail(user.getUsername());
            dto.setUserId(user.getUserId());
            dto.setFirstName(user.getFirstName());
            dto.setLastName(user.getLastName());
            dto.setEmail(user.getEmail());
            dto.setRole(user.getRole());
            dto.setEnabled(user.isEnabled());
            dto.setUsername(user.getRawUsername());


            String profilePictureFileName = user.getProfilePictureUrl();

            if (profilePictureFileName != null && !profilePictureFileName.isEmpty()) {

                String imageUrl = "/uploads/" + profilePictureFileName;
                dto.setProfilePictureUrl(imageUrl);
            } else {
                dto.setProfilePictureUrl(null);
            }

            return ResponseEntity.ok(dto);
        } else {
            return ResponseEntity.status(404).body(null);
        }
    }

    @GetMapping("/usernames")
    public List<String> getAllUsernames() {
        return userService.getAllUsernames();
    }

    @GetMapping("/{username}/details")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDetailsDto> getUserDetails(@PathVariable String username) {
        User user = userService.getUserEntityByUsername(username);

        UserDetailsDto dto = new UserDetailsDto();
        // Utilisez les mêmes noms de méthodes que dans votre version byId
        dto.setEmail(user.getEmail());  // Pas user.getUsername() comme dans byId
        dto.setUserId(user.getUserId()); // Vérifiez si c'est getId() ou getUserId()
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setRole(user.getRole());
        dto.setEnabled(user.isEnabled());
        dto.setUsername(user.getUsername()); // Vérifiez si c'est getUsername() ou getRawUsername()

        // Gestion de l'image cohérente avec byId
        String profilePictureFileName = user.getProfilePictureUrl();
        if (profilePictureFileName != null && !profilePictureFileName.isEmpty()) {
            String imageUrl = "/uploads/" + profilePictureFileName;
            dto.setProfilePictureUrl(imageUrl);
        } else {
            dto.setProfilePictureUrl(null);
        }

        return ResponseEntity.ok(dto);
    }

    @Autowired
    private EmailService emailService;  // Injection du service

    @PostMapping("/{userId}/ban")
    public ResponseEntity<BanResult> banUser(
            @PathVariable Long userId,
            @Valid @RequestBody BanRequestDto banRequest) {

        BanResult result = userService.banUser(userId, banRequest);

        if (result.isSuccess()) {
            Optional<User> userOptional = userRepository.findById(userId);

            userOptional.ifPresent(user -> {
                String durationMessage = result.getBanEndDate() != null
                        ? "Votre compte sera réactivé le: " + result.getBanEndDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm"))
                        : "Bannissement permanent";

                try {
                    emailService.sendEmail(  // Plus d'appel statique
                            user.getEmail(),
                            "Notification de bannissement - GameMax",
                            "Bonjour " + user.getUsername() + ",\n\n" +
                                    "Votre compte a été banni pour la raison suivante:\n" +
                                    "» " + result.getReason() + "\n\n" +
                                    durationMessage + "\n\n" +
                                    "Contactez notre support: support@gamax.com\n\n" +
                                    "Cordialement,\nL'équipe GameMax"
                    );
                    result.setEmailSent(true);
                } catch (Exception e) {
                    result.setEmailSent(false);
                    // Log l'erreur si besoin
                }
            });
        }

        return result.isSuccess()
                ? ResponseEntity.ok(result)
                : ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
    }
/*
    @GetMapping("/banned-users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<BannedUserResponse>> getBannedUsers(
            @RequestParam(required = false) BanDuration duration,
            @PageableDefault(size = 10, sort = "banEndDate", direction = Sort.Direction.ASC) Pageable pageable) {

        LocalDateTime maxDate = duration != null && !duration.equals(BanDuration.PERMANENT)
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
        );
    }
    private String calculateRemainingBanTime(User user) {
        if (user.getBanEndDate() == null) return "Permanent";

        Duration remaining = Duration.between(LocalDateTime.now(), user.getBanEndDate());
        return String.format("%dj %dh", remaining.toDays(), remaining.toHours() % 24);
    }


 */

    @GetMapping("/summary")
    public ResponseEntity<List<UserSummaryDTO>> getAllUsersSummary() {
        return ResponseEntity.ok(userService.getAllUsersSummary());
    }

    @GetMapping("/usernames/{username}")
    public ResponseEntity<UserDetailsDto> getUserByUsername(@PathVariable String username) {
        try {
            UserDetailsDto user = userService.findbyusername(username);
            return ResponseEntity.ok(user);
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/usernames/count")
    public ResponseEntity<Integer> countUsernames() {
        int count = userService.countUsernames();
        return ResponseEntity.ok(count);
    }

    @GetMapping("/count/new-last-week")
    public ResponseEntity<Long> countNewUsersLastWeek() {
        LocalDateTime oneWeekAgo = LocalDateTime.now().minusWeeks(1);
        System.out.println("Counting users created after: " + oneWeekAgo);
        long count = userRepository.countByCreatedAtAfter(oneWeekAgo);
        System.out.println("Found " + count + " new users in the last week");
        return ResponseEntity.ok(count);
    }

    @GetMapping("/new-customers/growth-percentage")
    // @PreAuthorize("hasRole('ADMIN')") // Sécurise l'accès si besoin
    public ResponseEntity<Double> getNewCustomersGrowthPercentage() {
        double growth = userService.calculateNewCustomersGrowthPercentage();
        return ResponseEntity.ok(growth);
    }

    @GetMapping("/count/new")

    public ResponseEntity<Long> countNewUsersByPeriod(@RequestParam String period) {
        try {
            long count = userService.countNewUsersByPeriod(period);
            return ResponseEntity.ok(count);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(0L);
        } catch (Exception e) {

            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(0L);
        }


    }
    @GetMapping("/stats/new-by-period")

    public ResponseEntity<Map<String, Long>> getNewUsersByPeriod(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "WEEKS") ChronoUnit unit)
    {
        Map<String, Long> data = userService.countUsersByPeriod(startDate, endDate, unit);
        return ResponseEntity.ok(data);
    }

}


