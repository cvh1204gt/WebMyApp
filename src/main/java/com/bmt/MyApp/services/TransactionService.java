package com.bmt.MyApp.services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.bmt.MyApp.models.AppUser;
import com.bmt.MyApp.models.Transactions;
import com.bmt.MyApp.models.Transactions.TransactionStatus;
import com.bmt.MyApp.repositories.TransactionsRepository;

@Service
public class TransactionService {

    @Autowired
    private TransactionsRepository transactionsRepository;

    public Page<Transactions> getAllTransactions(Pageable pageable) {
        return transactionsRepository.findAll(pageable);
    }

    public List<Transactions> getAllTransactions() {
        return transactionsRepository.findAll();
    }

    public Optional<Transactions> getTransactionById(Long id) {
        return transactionsRepository.findById(id);
    }

    public Transactions saveTransaction(Transactions transaction) {
        return transactionsRepository.save(transaction);
    }

    public void deleteTransaction(Long id) {
        transactionsRepository.deleteById(id);
    }

    public Optional<Transactions> findActiveTransactionByUserAndService(AppUser user, Long serviceId) {
        return transactionsRepository.findActiveTransactionByUserAndService(user, serviceId, LocalDateTime.now());
    }

    public long getTotalTransactions() {
        return transactionsRepository.count();
    }

    public Page<Transactions> searchTransactions(String search, String startDate, String endDate,
                                                 String expireStartDate, String expireEndDate, Pageable pageable) {

        LocalDateTime startDateTime = null;
        LocalDateTime endDateTime = null;
        LocalDateTime expireStartDateTime = null;
        LocalDateTime expireEndDateTime = null;

        try {
            if (startDate != null && !startDate.trim().isEmpty()) {
                startDateTime = LocalDate.parse(startDate).atStartOfDay();
            }
            if (endDate != null && !endDate.trim().isEmpty()) {
                endDateTime = LocalDate.parse(endDate).atTime(23, 59, 59);
            }
            if (expireStartDate != null && !expireStartDate.trim().isEmpty()) {
                expireStartDateTime = LocalDate.parse(expireStartDate).atStartOfDay();
            }
            if (expireEndDate != null && !expireEndDate.trim().isEmpty()) {
                expireEndDateTime = LocalDate.parse(expireEndDate).atTime(23, 59, 59);
            }
        } catch (Exception e) {
            // ignore parse error
        }

        return transactionsRepository.searchTransactions(
            search, startDateTime, endDateTime, expireStartDateTime, expireEndDateTime, pageable);
    }

    public List<Transactions> searchTransactionsForExport(String search, String startDate, String endDate,
                                                          String expireStartDate, String expireEndDate) {
        LocalDateTime startDateTime = null;
        LocalDateTime endDateTime = null;
        LocalDateTime expireStartDateTime = null;
        LocalDateTime expireEndDateTime = null;

        try {
            if (startDate != null && !startDate.trim().isEmpty()) {
                startDateTime = LocalDate.parse(startDate).atStartOfDay();
            }
            if (endDate != null && !endDate.trim().isEmpty()) {
                endDateTime = LocalDate.parse(endDate).atTime(23, 59, 59);
            }
            if (expireStartDate != null && !expireStartDate.trim().isEmpty()) {
                expireStartDateTime = LocalDate.parse(expireStartDate).atStartOfDay();
            }
            if (expireEndDate != null && !expireEndDate.trim().isEmpty()) {
                expireEndDateTime = LocalDate.parse(expireEndDate).atTime(23, 59, 59);
            }
        } catch (Exception e) {
            // ignore parse error
        }

        return transactionsRepository.searchTransactionsForExport(
            search, startDateTime, endDateTime, expireStartDateTime, expireEndDateTime);
    }

    // Thống kê
    public long countTransactionsByStatus(TransactionStatus status) {
        return transactionsRepository.countByStatus(status);
    }

    public BigDecimal sumAmountByStatus(TransactionStatus status) {
        return transactionsRepository.sumAmountByStatus(status);
    }

    public BigDecimal sumByStatusAndDateRange(TransactionStatus status, LocalDateTime start, LocalDateTime end) {
        return transactionsRepository.sumByStatusAndDateRange(status, start, end);
    }

    public long countByStatusAndDateRange(TransactionStatus status, LocalDateTime start, LocalDateTime end) {
        return transactionsRepository.countByStatusAndDateRange(status, start, end);
    }

    // Lấy giao dịch theo tên người dùng (cho user hiện tại)
    public Page<Transactions> findByUsername(String username, Pageable pageable) {
        return transactionsRepository.findByUsername(username, pageable);
    }
}