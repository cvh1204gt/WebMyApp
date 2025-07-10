package com.bmt.MyApp.services;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.bmt.MyApp.models.SystemLog;
import com.bmt.MyApp.repositories.SystemLogRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LogService {

    private final SystemLogRepository systemLogRepository;

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
