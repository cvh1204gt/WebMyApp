package com.bmt.MyApp.repositories;

import java.math.BigDecimal;
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
import com.bmt.MyApp.models.Services;
import com.bmt.MyApp.models.Transactions;
import com.bmt.MyApp.models.Transactions.TransactionStatus;

@Repository
public interface TransactionsRepository extends JpaRepository<Transactions, Long> {

    // === Giao dịch đang hoạt động của user + dịch vụ ===
    @Query("SELECT t FROM Transactions t WHERE t.user = :user AND t.service.id = :serviceId " +
           "AND t.status = 'SUCCESS' AND t.expireDate > :currentTime")
    Optional<Transactions> findActiveTransactionByUserAndService(@Param("user") AppUser user,
                                                                  @Param("serviceId") Long serviceId,
                                                                  @Param("currentTime") LocalDateTime currentTime);

    List<Transactions> findByUser(AppUser user);

    List<Transactions> findByStatus(TransactionStatus status);

    Optional<Transactions> findByVnpTxnRef(String vnpTxnRef);

    @EntityGraph(attributePaths = {"user", "service"})
    Page<Transactions> findAll(Pageable pageable);

    @Query("SELECT t FROM Transactions t JOIN FETCH t.user JOIN FETCH t.service")
    List<Transactions> findAllWithUserAndService();

    // === Tìm kiếm giao dịch nâng cao (có phân trang) ===
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

    // === Tìm kiếm giao dịch để export (không phân trang) ===
    @Query("SELECT t FROM Transactions t " +
           "WHERE (:search IS NULL OR LOWER(t.user.fullName) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND (:startDate IS NULL OR t.createdAt >= :startDate) " +
           "AND (:endDate IS NULL OR t.createdAt <= :endDate) " +
           "AND (:expireStartDate IS NULL OR t.expireDate >= :expireStartDate) " +
           "AND (:expireEndDate IS NULL OR t.expireDate <= :expireEndDate) " +
           "ORDER BY t.createdAt DESC")
    List<Transactions> searchTransactionsForExport(
        @Param("search") String search,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        @Param("expireStartDate") LocalDateTime expireStartDate,
        @Param("expireEndDate") LocalDateTime expireEndDate
    );

    // === Thống kê ===
    long countByStatus(TransactionStatus status);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transactions t WHERE t.status = :status")
    BigDecimal sumAmountByStatus(@Param("status") TransactionStatus status);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transactions t WHERE " +
           "t.status = :status AND t.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal sumByStatusAndDateRange(@Param("status") TransactionStatus status,
                                       @Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(t) FROM Transactions t WHERE t.status = :status AND t.createdAt BETWEEN :startDate AND :endDate")
    long countByStatusAndDateRange(@Param("status") TransactionStatus status,
                                   @Param("startDate") LocalDateTime start,
                                   @Param("endDate") LocalDateTime end);

    // === Lấy các gói dịch vụ đã mua thành công của user ===
    @Query("SELECT DISTINCT t.service FROM Transactions t WHERE t.user = :user " +
           "AND t.status = 'SUCCESS' AND t.expireDate > :currentTime")
    List<Services> findPurchasedServicesByUser(@Param("user") AppUser user, 
                                               @Param("currentTime") LocalDateTime currentTime);

    // === Giao dịch của chính người dùng đang đăng nhập ===
    @Query("SELECT t FROM Transactions t WHERE t.user.email = :email ORDER BY t.createdAt DESC")
    Page<Transactions> findByEmail(@Param("email") String email, Pageable pageable);

    // === Lấy tất cả giao dịch của user theo email (không phân trang) ===
    @Query("SELECT t FROM Transactions t WHERE t.user.email = :email ORDER BY t.createdAt DESC")
    List<Transactions> findByUserEmailOrderByCreatedAtDesc(@Param("email") String email);

    List<Transactions> findByUserAndStatus(AppUser user, Transactions.TransactionStatus status);
}