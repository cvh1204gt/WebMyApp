package com.bmt.MyApp.controllers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.bmt.MyApp.models.SystemLog;
import com.bmt.MyApp.repositories.SystemLogRepository;

@Controller
public class LogController {

    @Autowired
    private SystemLogRepository logRepository;

    @GetMapping("/logs")
    public String viewLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            Model model) {
        
        // Tạo Pageable với sắp xếp theo timestamp giảm dần
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        
        Page<SystemLog> logsPage;
        
        // Xử lý tìm kiếm và lọc theo ngày
        LocalDateTime startDateTime = null;
        LocalDateTime endDateTime = null;
        
        if (startDate != null && !startDate.isEmpty()) {
            startDateTime = LocalDate.parse(startDate).atStartOfDay();
        }
        
        if (endDate != null && !endDate.isEmpty()) {
            endDateTime = LocalDate.parse(endDate).atTime(LocalTime.MAX);
        }
        
        // Thực hiện tìm kiếm dựa trên các điều kiện
        if ((search != null && !search.trim().isEmpty()) || startDateTime != null || endDateTime != null) {
            logsPage = logRepository.findLogsWithFilters(
                search != null ? search.trim() : null,
                startDateTime,
                endDateTime,
                pageable
            );
        } else {
            logsPage = logRepository.findAll(pageable);
        }
        
        // Thêm thông tin vào model
        model.addAttribute("logs", logsPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", logsPage.getTotalPages());
        model.addAttribute("totalElements", logsPage.getTotalElements());
        model.addAttribute("hasPrevious", logsPage.hasPrevious());
        model.addAttribute("hasNext", logsPage.hasNext());
        model.addAttribute("size", size);
        
        // Giữ lại các tham số tìm kiếm
        model.addAttribute("search", search);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        
        return "log";
    }
}