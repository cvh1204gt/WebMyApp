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

/**
 * Controller for displaying available services to the user.
 */
@Controller
@RequestMapping("/services")
public class ServiceController {

    @Autowired
    private ServicesRepository servicesRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    /**
     * Displays the list of services available for the current user to purchase.
     * @param model the Spring MVC model
     * @return the services view template
     */
    @GetMapping
    public String getAllServices(Model model) {
        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        AppUser currentUser = appUserRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        List<Services> availableServices = servicesRepository.findAvailableServicesForUser(
                currentUser, LocalDateTime.now());
        model.addAttribute("services", availableServices);
        return "services";
    }
}