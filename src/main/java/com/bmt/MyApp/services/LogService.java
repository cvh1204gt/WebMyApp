package com.bmt.MyApp.services;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.bmt.MyApp.models.SystemLog;
import com.bmt.MyApp.repositories.SystemLogRepository;

import lombok.RequiredArgsConstructor;

/**
 * Service for logging system actions to the SystemLogRepository.
 */
@Service
@RequiredArgsConstructor
public class LogService {

    private final SystemLogRepository systemLogRepository;

    /**
     * Logs an action performed by a user.
     *
     * @param username the username performing the action
     * @param action the action performed
     * @param detail details about the action
     */
    public void log(String username, String action, String detail) {
        SystemLog log = SystemLog.builder()
            .username(username)
            .action(action)
            .detail(detail)
            .timestamp(LocalDateTime.now())
            .build();
        systemLogRepository.save(log);
    }
}
