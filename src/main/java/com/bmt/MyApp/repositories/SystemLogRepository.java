package com.bmt.MyApp.repositories;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bmt.MyApp.models.SystemLog;

public interface SystemLogRepository extends JpaRepository<SystemLog, Long> {

    // Tìm kiếm theo username, action hoặc detail
    @Query("SELECT sl FROM SystemLog sl WHERE " +
           "(:search IS NULL OR " +
           "LOWER(sl.username) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(sl.action) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(sl.detail) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:startDate IS NULL OR sl.timestamp >= :startDate) AND " +
           "(:endDate IS NULL OR sl.timestamp <= :endDate)")
    Page<SystemLog> findLogsWithFilters(
        @Param("search") String search,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );

    // Tìm kiếm chỉ theo username
    Page<SystemLog> findByUsernameContainingIgnoreCase(String username, Pageable pageable);

    // Tìm kiếm chỉ theo action
    Page<SystemLog> findByActionContainingIgnoreCase(String action, Pageable pageable);

    // Tìm kiếm theo khoảng thời gian
    Page<SystemLog> findByTimestampBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
}