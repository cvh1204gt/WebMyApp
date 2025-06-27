package com.bmt.MyApp.repositories;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
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
    
    // Tìm giao dịch theo user
    List<Transactions> findByUser(AppUser user);
    
    // Tìm giao dịch theo status
    List<Transactions> findByStatus(Transactions.TransactionStatus status);
    
    // Tìm giao dịch theo khoảng thời gian
    @Query("SELECT t FROM Transactions t WHERE t.createdAt BETWEEN :startDate AND :endDate")
    List<Transactions> findByDateRange(@Param("startDate") LocalDateTime startDate, 
                                     @Param("endDate") LocalDateTime endDate);
    
    // Tìm giao dịch theo vnpTxnRef
    Optional<Transactions> findByVnpTxnRef(String vnpTxnRef);
    
    // Tìm giao dịch với phân trang và sắp xếp (eager loading user và service)
    @EntityGraph(attributePaths = {"user", "service"})
    Page<Transactions> findAll(Pageable pageable);
    
    @Query("SELECT t FROM Transactions t JOIN FETCH t.user JOIN FETCH t.service")
    List<Transactions> findAllWithUserAndService();
    
    // Tìm kiếm giao dịch với nhiều điều kiện
    @Query("SELECT t FROM Transactions t WHERE " +
           "(:search IS NULL OR :search = '' OR LOWER(t.user.fullName) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:startDate IS NULL OR t.createdAt >= :startDate) AND " +
           "(:endDate IS NULL OR t.createdAt <= :endDate) AND " +
           "(:expireStartDate IS NULL OR t.expireDate >= :expireStartDate) AND " +
           "(:expireEndDate IS NULL OR t.expireDate <= :expireEndDate)")
    @EntityGraph(attributePaths = {"user", "service"})
    Page<Transactions> searchTransactions(
        @Param("search") String search,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        @Param("expireStartDate") LocalDateTime expireStartDate,
        @Param("expireEndDate") LocalDateTime expireEndDate,
        Pageable pageable
    );
}