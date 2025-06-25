package com.bmt.MyApp.controllers;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.bmt.MyApp.models.AppUser;
import com.bmt.MyApp.models.Services;
import com.bmt.MyApp.repositories.AppUserRepository;
import com.bmt.MyApp.repositories.ServicesRepository;

@Controller
@RequestMapping("/services")
public class ServiceController {
  
    @Autowired
    private ServicesRepository servicesRepository;
    
    @Autowired
    private AppUserRepository appUserRepository;

    @GetMapping
    public String getAllServices(Model model) {
        // Lấy thông tin user hiện tại
        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        AppUser currentUser = appUserRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Lấy danh sách services mà user có thể mua (chưa mua hoặc đã hết hạn)
        List<Services> availableServices = servicesRepository.findAvailableServicesForUser(
                currentUser, LocalDateTime.now());
        
        model.addAttribute("services", availableServices);
        return "services"; // trỏ đến file: templates/services.html
    }
}