package com.bmt.MyApp.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.bmt.MyApp.models.ServicePack;
import com.bmt.MyApp.services.ServicePackService;

@ControllerAdvice
public class GlobalModelController {
    @Autowired
    private ServicePackService service;

    @ModelAttribute("packs")
    public List<ServicePack> allServicePacks() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMINDICHVU"))) {
            return service.searchPaged("", 0, 1000).getContent();
        }
        return null;
    }
} 