package com.bmt.MyApp.repositories;

import com.bmt.MyApp.models.AppUser;
import com.bmt.MyApp.models.Services;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ServicesRepository extends JpaRepository<Services, Long> {

  // Optional<Services> findById(Integer Id);

  @Query("SELECT s FROM Services s WHERE s.id NOT IN " +
      "(SELECT DISTINCT t.service.id FROM Transactions t " +
      "WHERE t.user = :user AND t.status = 'SUCCESS' AND t.expireDate > :currentTime)")
  List<Services> findAvailableServicesForUser(@Param("user") AppUser user,
      @Param("currentTime") LocalDateTime currentTime);
}
