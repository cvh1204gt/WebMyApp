package com.bmt.MyApp.controllers;

import org.springframework.beans.factory.annotation.Autowired;
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
public class HomeController {

  @Autowired
  private AppUserService appUserService;

  @GetMapping({ "", "/" })
  public String home() {
    return "index";
  }

  @GetMapping("/home")
  public String dashboard() {
    return "home";
  }

  @GetMapping("/profile")
  public String profile(Model model) {
    AppUser currentUser = appUserService.getCurrentUser();

    if (currentUser == null) {
      return "redirect:/login";
    }

    model.addAttribute("user", currentUser);

    return "profile";
  }


  @PostMapping("/profile/update")
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

  // @PreAuthorize("hasRole('CLIENT')")
  // @GetMapping("/services")
  // public String services() {
  //   return "services";
  // }

  //ADMIN
  @PreAuthorize("hasRole('ADMIN')")
  @GetMapping("/account_management")
  public String accountManagement() {
    return "account_management";
  }

  @PreAuthorize("hasRole('ADMIN')")
  @GetMapping("/lichsugiaodich")
  public String lichSuGiaoDich() {
    return "lichsugiaodich";
  }
}
