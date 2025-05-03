package com.gamax.userservice.Repository;

import com.gamax.userservice.Entity.BanHistory;
import feign.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface BanHistoryRepository extends JpaRepository<BanHistory, Long> {
    @Query("SELECT h FROM BanHistory h WHERE h.userId = :userId ORDER BY h.banDate DESC")
    List<BanHistory> findByUserIdOrderByBanDateDesc(@Param("userId") Long userId);
}