package com.bmt.MyApp.repositories;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.bmt.MyApp.models.AppUser;
import com.bmt.MyApp.models.Transactions;

@Repository
public interface TransactionsRepository extends JpaRepository<Transactions, Long> {
  @Query("SELECT t FROM Transactions t WHERE t.user = :user AND t.service.id = :serviceId " +
      "AND t.status = 'SUCCESS' AND t.expireDate > :currentTime")
  Optional<Transactions> findActiveTransactionByUserAndService(@Param("user") AppUser user,
      @Param("serviceId") Long serviceId,
      @Param("currentTime") LocalDateTime currentTime);
}
