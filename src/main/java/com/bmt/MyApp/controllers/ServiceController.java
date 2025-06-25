package com.bmt.MyApp.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.bmt.MyApp.models.Services;
import com.bmt.MyApp.repositories.ServicesRepository;


@Controller
@RequestMapping("/services")
public class ServiceController {
  
  @Autowired
    private ServicesRepository servicesRepository;

    @GetMapping
    public String getAllServices(Model model) {
        List<Services> services = servicesRepository.findAll();
        model.addAttribute("services", services);
        return "services"; // trỏ đến file: templates/services/list.html
    }
}
