package com.bmt.MyApp.services;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.bmt.MyApp.models.AppUser;
import com.bmt.MyApp.models.Transactions;
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
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        
        try {
            if (startDate != null && !startDate.trim().isEmpty()) {
                startDateTime = LocalDateTime.parse(startDate + "T00:00:00");
            }
            if (endDate != null && !endDate.trim().isEmpty()) {
                endDateTime = LocalDateTime.parse(endDate + "T23:59:59");
            }
            if (expireStartDate != null && !expireStartDate.trim().isEmpty()) {
                expireStartDateTime = LocalDateTime.parse(expireStartDate + "T00:00:00");
            }
            if (expireEndDate != null && !expireEndDate.trim().isEmpty()) {
                expireEndDateTime = LocalDateTime.parse(expireEndDate + "T23:59:59");
            }
        } catch (Exception e) {
            // Nếu có lỗi parse date, bỏ qua và tìm kiếm không có điều kiện date
        }
        
        return transactionsRepository.searchTransactions(
            search, startDateTime, endDateTime, expireStartDateTime, expireEndDateTime, pageable);
    }
}