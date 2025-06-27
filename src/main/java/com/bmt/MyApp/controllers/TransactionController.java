package com.bmt.MyApp.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.bmt.MyApp.models.Transactions;
import com.bmt.MyApp.services.TransactionService;

@Controller
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    @GetMapping("/lichsugiaodich")
    public String lichSuGiaoDich(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String expireStartDate,
            @RequestParam(required = false) String expireEndDate,
            Model model) {
        
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
            Sort.by(sortBy).descending() : 
            Sort.by(sortBy).ascending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Transactions> transactionPage = transactionService.searchTransactions(
            search, startDate, endDate, expireStartDate, expireEndDate, pageable);
        
        model.addAttribute("transactions", transactionPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", transactionPage.getTotalPages());
        model.addAttribute("totalElements", transactionPage.getTotalElements());
        model.addAttribute("hasNext", transactionPage.hasNext());
        model.addAttribute("hasPrevious", transactionPage.hasPrevious());
        
        // Giữ lại các giá trị tìm kiếm
        model.addAttribute("search", search);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("expireStartDate", expireStartDate);
        model.addAttribute("expireEndDate", expireEndDate);
        
        return "lichsugiaodich";
    }
}