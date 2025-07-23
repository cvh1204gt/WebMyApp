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
import com.bmt.MyApp.services.LogService;

/**
 * Controller for viewing and searching system logs.
 */
@Controller
public class LogController {

    @Autowired
    private SystemLogRepository logRepository;

    @Autowired
    private LogService logService;

    /**
     * Displays the logs page with optional search and date filters.
     *
     * @param page the page number
     * @param size the page size
     * @param search the search keyword
     * @param startDate the start date filter
     * @param endDate the end date filter
     * @param model the Spring MVC model
     * @return the log view template
     */
    @GetMapping("/logs")
    public String viewLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            Model model) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<SystemLog> logsPage;
        LocalDateTime startDateTime = null;
        LocalDateTime endDateTime = null;
        if (startDate != null && !startDate.isEmpty()) {
            startDateTime = LocalDate.parse(startDate).atStartOfDay();
        }
        if (endDate != null && !endDate.isEmpty()) {
            endDateTime = LocalDate.parse(endDate).atTime(LocalTime.MAX);
        }
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
        model.addAttribute("logs", logsPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", logsPage.getTotalPages());
        model.addAttribute("totalElements", logsPage.getTotalElements());
        model.addAttribute("hasPrevious", logsPage.hasPrevious());
        model.addAttribute("hasNext", logsPage.hasNext());
        model.addAttribute("size", size);
        model.addAttribute("search", search);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        return "log";
    }
}