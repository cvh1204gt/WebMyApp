package com.bmt.MyApp.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.bmt.MyApp.models.AppUser;
import com.bmt.MyApp.services.AppUserService;


@Controller
/**
 * Handles home, profile, and admin-related endpoints.
 */
public class HomeController {

  @Autowired
  private AppUserService appUserService;

  @GetMapping({ "", "/" })
  /**
   * Displays the index page.
   */
  public String home() {
    return "index";
  }

  @GetMapping("/profile")
  /**
   * Displays the profile page for the current user.
   */
  public String profile(Model model) {
    AppUser currentUser = appUserService.getCurrentUser();

    if (currentUser == null) {
      return "redirect:/login";
    }

    model.addAttribute("user", currentUser);

    return "profile";
  }


  @PostMapping("/profile/update")
  /**
   * Updates the profile information for the current user.
   */
  public String updateProfile(@ModelAttribute("user") AppUser user, 
                             RedirectAttributes redirectAttributes) {
    try {
      // Cập nhật thông tin user
      AppUser updatedUser = appUserService.updateCurrentUserProfile(user);
      
      if (updatedUser != null) {
        redirectAttributes.addFlashAttribute("successMessage", 
          "Cập nhật thông tin thành công!");
      } else {
        redirectAttributes.addFlashAttribute("errorMessage", 
          "Không thể cập nhật thông tin. Vui lòng thử lại!");
      }
    } catch (Exception e) {
      redirectAttributes.addFlashAttribute("errorMessage", 
        "Có lỗi xảy ra: " + e.getMessage());
    }
    
    return "redirect:/profile";
  }

  //ADMIN
  @PreAuthorize("hasRole('ADMIN')")
  @GetMapping("/account_management")
  public String accountManagement() {
    return "account_management";
  }


  @PreAuthorize("hasRole('ADMIN')")
  @GetMapping("/log")
  public String log() {
    return "log";
  }


  @GetMapping("/recharge")
  /**
   * Displays the recharge page.
   */
  public String recharge() {
    return "recharge";
  }



    @GetMapping("/healthz")
  /**
   * Health check endpoint.
   */
  public ResponseEntity<String> healthz() {
      return ResponseEntity.ok("OK");
  }


}
