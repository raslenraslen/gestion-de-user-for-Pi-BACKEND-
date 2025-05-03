package com.gamax.userservice.Service;

import com.gamax.userservice.Entity.User;
import com.gamax.userservice.Repository.UserRepository;
import com.gamax.userservice.TDO.*;

import com.gamax.userservice.enums.BanDuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserService extends BaseService<User> {
    @Autowired
    private UserRepository userRepository;
    @Override
    protected JpaRepository<User, Long> getRepository() {
        return userRepository;
    }

    public UserDTO getTDObyID(Long userId) {
        try {
            User user = getRepository().getById(userId);
            return converToTDO(user);
        }catch (Exception e){
            return null;
        }
    }
    private UserDTO converToTDO(User user){
        return UserDTO.builder()
                .id(user.getUserId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .build();
    }
    public List<UserDTO> getTDDOUsersByIds(List<Long> userIds) {
        List<User>users= getRepository().findAllById(userIds);
        return users.stream()
                .map(this::converToTDO)
                .collect(Collectors.toList());
    }
    public List<User> allUsers() {

        List<User> users = new ArrayList<>(userRepository.findAll());

        return users;
    }
    public Optional<User> getUserById(Long userId) {
        return userRepository.findById(userId);
    }

    // Ajout de la méthode saveUser
    public User saveUser(User user) {
        if(user.getCreatedAt() == null) {
            user.setCreatedAt(LocalDateTime.now());
        }
        return userRepository.save(user);
    }

    public List<String> getAllUsernames() {
        return userRepository.findAllUsernamesAsStrings();  // ou findAllUsernames() pour le DTO
    }


    public User getUserEntityByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé"));
    }

    public BanResult banUser(Long userId, BanRequestDto banRequest) {
        Optional<User> userOpt = userRepository.findById(userId);

        if (userOpt.isEmpty()) {
            return new BanResult(userId); // Retourne un échec
        }

        User user = userOpt.get();

        // Appliquer le ban
        user.setBanned(true);
        user.setBanReason(banRequest.getReason());

        if (banRequest.getDuration() == BanDuration.PERMANENT) {
            user.setBanEndDate(null);
        } else {
            user.setBanEndDate(LocalDateTime.now().plusDays(banRequest.getDuration().getDays()));
        }

        userRepository.save(user);


        return new BanResult(
                userId,
                banRequest.getDuration().toString(),
                banRequest.getReason(),
                user.getBanEndDate()
        );
    }

    public List<UserSummaryDTO> getAllUsersSummary() {
        List<User> users = userRepository.findAll();

        return users.stream()
                .map(user -> {
                    UserSummaryDTO dto = new UserSummaryDTO();
                    dto.setUserId(user.getUserId());
                    dto.setUsername(user.getRawUsername()); // Utilise getRawUsername()
                    dto.setEmail(user.getEmail());
                    dto.setFullName(user.getFirstName() + " " + user.getLastName());
                    dto.setAccountCreationDate(user.getAccountCreationDate());
                    dto.setProfilePictureUrl(user.getProfilePictureUrl()); // Optionnel
                    return dto;
                })
                .collect(Collectors.toList());
    }
    public UserDetailsDto findbyusername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        UserDetailsDto dto = new UserDetailsDto();
        dto.setEmail(user.getEmail());
        dto.setUserId(user.getUserId());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setRole(user.getRole());
        dto.setEnabled(user.isEnabled());
        dto.setUsername(user.getRawUsername());
        dto.setAge(user.getAge());
        dto.setBirthday(user.getBirthday());
        dto.setAccountCreationDate(user.getAccountCreationDate());
        dto.setProfilePictureUrl(user.getProfilePictureUrl());


        return dto;
    }
    public int countUsernames() {
        return userRepository.countUsernames();
    }

    public double calculateNewCustomersGrowthPercentage() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startLastWeek = now.minusDays(7).truncatedTo(ChronoUnit.DAYS);
        long countLastWeek = userRepository.countByCreatedAtAfter(startLastWeek);

        LocalDateTime startPreviousWeek = now.minusDays(14).truncatedTo(ChronoUnit.DAYS);
        long countPreviousWeek = userRepository.countByCreatedAtAfter(startPreviousWeek) - countLastWeek;

        if (countPreviousWeek == 0) {
            return (countLastWeek > 0) ? 100.0 : 0.0;
        }

        double growth = ((double) (countLastWeek - countPreviousWeek) / countPreviousWeek) * 100.0;
        return Math.round(growth * 100.0) / 100.0;
    }

    private LocalDateTime calculateStartDate(String period) {
        LocalDateTime now = LocalDateTime.now();
        switch (period.toLowerCase()) {
            case "day":
                return now.minusDays(1); // Dernières 24h
            case "week":
                return now.minusWeeks(1); // Derniers 7 jours
            case "month":
                return now.minusMonths(1); // Dernier mois
            // Ajouter d'autres cas si besoin (quarter, year, etc.)
            default:
                // Gérer le cas où la période n'est pas reconnue
                throw new IllegalArgumentException("Période '" + period + "' non supportée.");
        }
    }


    public long countNewUsersByPeriod(String period) {
        LocalDateTime startDate = calculateStartDate(period);
        return userRepository.countByCreatedAtAfter(startDate);
    }

    public Map<String, Long> countUsersByPeriod(LocalDate startDate, LocalDate endDate, ChronoUnit unit) {
        List<User> users = userRepository.findByCreatedAtBetween(startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay());

        Map<String, Long> counts = new LinkedHashMap<>();

        users.forEach(user -> {
            LocalDate date = user.getCreatedAt().toLocalDate();
            LocalDate periodStart;

            if (unit == ChronoUnit.WEEKS) {
                periodStart = date.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)); // Assuming Monday is start of week
            } else if (unit == ChronoUnit.MONTHS) {
                periodStart = date.withDayOfMonth(1);
            } else { // Default to DAYS
                periodStart = date;
            }

            String key = periodStart.toString();
            counts.merge(key, 1L, Long::sum);
        });

        return counts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue,
                        LinkedHashMap::new
                ));
    }



    }

