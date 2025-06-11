package com.bmt.MyApp.controllers;

import java.util.Date;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import com.bmt.MyApp.models.AppUser;
import com.bmt.MyApp.models.RegisterDto;
import com.bmt.MyApp.repositories.AppUserRepository;

import jakarta.validation.Valid;

@Controller
public class AccountController {
  @Autowired
  private AppUserRepository repo;

  @GetMapping("/login")
  public String login() {
    return "login";
  }

  @GetMapping("/register")
  public String register(Model model) {
    RegisterDto registerDto = new RegisterDto();
    model.addAttribute(registerDto);
    model.addAttribute("success", false);
    return "register";
  }

  @PostMapping("/register")
  public String register(Model model, @Valid @ModelAttribute RegisterDto registerDto, BindingResult result) {

    if(!registerDto.getPassword().equals(registerDto.getConfirmPassword())) {
      result.addError(
              new FieldError("registerDto", "confirmPassword", "Nhập lại mật khẩu không chính xác, mời nhập lại!")
      );
    }

    Optional<AppUser> appUser = repo.findByEmail(registerDto.getEmail());
    if(appUser.isPresent()) {
      result.addError(
              new FieldError("registerDto", "email", "Email đã tồn tại, mời nhập lại!")
      );
    }

    if(result.hasErrors()) {
      return "register";
    }

    try {
      var bCryptEncoder = new BCryptPasswordEncoder();

      AppUser newUser = new AppUser();
      newUser.setFullName(registerDto.getFullName());
      newUser.setEmail(registerDto.getEmail());
      newUser.setRole("client");
      newUser.setCreateAt(new Date());
      newUser.setPassword(bCryptEncoder.encode(registerDto.getPassword()));

      repo.save(newUser);

      model.addAttribute("registerDto", new RegisterDto());
      model.addAttribute("success", true);

    } catch(Exception ex) {
      result.addError(
              new FieldError("registerDto", "fullname", ex.getMessage())
      );    
    }

    return "register";
  }
}
