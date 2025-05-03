package com.gamax.userservice.Repository;

import com.gamax.userservice.Entity.User;
import com.gamax.userservice.enums.BanDuration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // Méthodes existantes (conservées)
    Optional<User> findByEmail(String email);
    Optional<User> findByActivationToken(String token);
    Optional<User> findByResetToken(String resetToken);
    Optional<User> findById(Long id);

    @Query("SELECT u.username FROM User u")
    List<String> findAllUsernamesAsStrings();

    Optional<User> findByUsername(String username);

    @Query("SELECT COUNT(u.username) FROM User u")
    int countUsernames();
    @Query("SELECT u FROM User u WHERE u.isBanned = true AND " +
            "(:duration IS NULL OR " +
            "(:duration = 'PERMANENT' AND u.banEndDate IS NULL) OR " +
            "(u.banEndDate > CURRENT_TIMESTAMP AND u.banEndDate < :maxDate))")
    Page<User> findBannedUsersFiltered(
            @Param("duration") BanDuration duration,
            @Param("maxDate") LocalDateTime maxDate,
            Pageable pageable);

    long countByCreatedAtAfter(LocalDateTime date);
    long count();

    // Compte les utilisateurs actifs (non bannis)
    long countByIsBannedFalse();

    // Compte les utilisateurs bannis
    long countByIsBannedTrue();
    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    @Query("SELECT COUNT(u) FROM User u " +
            "WHERE u.createdAt BETWEEN :cohortStart AND :cohortEnd " +
            "AND u.lastActiveAt BETWEEN :activityStart AND :activityEnd")
    long countActiveInPeriod(
            @Param("cohortStart") LocalDateTime cohortStart,
            @Param("cohortEnd") LocalDateTime cohortEnd,
            @Param("activityStart") LocalDateTime activityStart,
            @Param("activityEnd") LocalDateTime activityEnd
    );
    List<User> findByCreatedAtBetween(LocalDateTime startDateTime, LocalDateTime endDateTime);



}


