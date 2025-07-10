package com.bmt.MyApp.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.bmt.MyApp.models.AppUser;
import com.bmt.MyApp.services.AppUserService;

@Controller
public class DashboardController {

    @Autowired
    private AppUserService appUserService;

    @GetMapping("/home")
    public String showDashboard(Model model) {
        AppUser currentUser = appUserService.getCurrentUser();
        model.addAttribute("user", currentUser);
        return "home";
    }
}
