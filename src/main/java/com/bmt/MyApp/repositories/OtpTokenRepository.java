package com.bmt.MyApp.repositories;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.bmt.MyApp.models.OtpToken;

import jakarta.transaction.Transactional;

@Repository
public interface OtpTokenRepository extends JpaRepository<OtpToken, Long> {
    
    Optional<OtpToken> findByEmailAndOtp(String email, String otp);
    
    Optional<OtpToken> findByEmail(String email);
    
    @Modifying
    @Transactional
    @Query("DELETE FROM OtpToken o WHERE o.email = :email")
    void deleteByEmail(@Param("email") String email);
    
    @Modifying
    @Transactional
    @Query("DELETE FROM OtpToken o WHERE o.expiryTime < :currentTime")
    void deleteByExpiryTimeBefore(@Param("currentTime") LocalDateTime currentTime);
}