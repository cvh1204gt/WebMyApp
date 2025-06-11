package com.bmt.MyApp.controllers;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

  @GetMapping({ "", "/" })
  public String home() {
    return "index";
  }

  @GetMapping("/home")
  public String dashboard() {
    return "home";
  }

  @PreAuthorize("hasRole('ADMIN')")
  @GetMapping("/profile")
  public String profile() {
    return "profile";
  }

  @GetMapping("/services")
  public String services() {
    return "services";
  }

  @GetMapping("/account_management")
  public String accountManagement() {
    return "account_management";
  }

  @GetMapping("/lichsugiaodich")
  public String lichSuGiaoDich() {
    return "lichsugiaodich";
  }
}
