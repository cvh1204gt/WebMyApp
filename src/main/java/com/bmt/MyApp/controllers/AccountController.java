package com.bmt.MyApp.controllers;

import java.util.Date;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.bmt.MyApp.models.AppUser;
import com.bmt.MyApp.repositories.AppUserRepository;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AccountController {

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/account_management")
    public String accountManagement(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "") String search,
            Model model) {
        
        Pageable pageable = PageRequest.of(page, 10);
        Page<AppUser> userPage;
        
        if (search.isEmpty()) {
            userPage = appUserRepository.findAll(pageable);
        } else {
            userPage = appUserRepository.findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                search, search, pageable);
        }
        
        model.addAttribute("users", userPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", userPage.getTotalPages());
        model.addAttribute("totalElements", userPage.getTotalElements());
        model.addAttribute("hasPrevious", userPage.hasPrevious());
        model.addAttribute("hasNext", userPage.hasNext());
        model.addAttribute("search", search);
        
        return "account_management";
    }

    @GetMapping("/add_account")
    public String showAddAccountForm(Model model) {
        model.addAttribute("user", new AppUser());
        model.addAttribute("isEdit", false);
        return "add_edit_account";
    }

    @PostMapping("/add_account")
    public String addAccount(@ModelAttribute AppUser user, RedirectAttributes redirectAttributes) {
        try {
            // Kiểm tra email đã tồn tại
            if (appUserRepository.existsByEmail(user.getEmail())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Email đã tồn tại!");
                return "redirect:/admin/add_account";
            }

            // Kiểm tra username đã tồn tại
            if (user.getUsername() != null && !user.getUsername().isEmpty() 
                && appUserRepository.existsByUsername(user.getUsername())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Username đã tồn tại!");
                return "redirect:/admin/add_account";
            }

            // Mã hóa password
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            user.setCreateAt(new Date());
            user.setVerified(true); // Admin tạo thì tự động verify
            
            // Đặt role mặc định nếu chưa có
            if (user.getRole() == null || user.getRole().isEmpty()) {
                user.setRole("CLIENT");
            }

            appUserRepository.save(user);
            redirectAttributes.addFlashAttribute("successMessage", "Thêm tài khoản thành công!");
            return "redirect:/admin/account_management";
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra: " + e.getMessage());
            return "redirect:/admin/add_account";
        }
    }

    @GetMapping("/edit_account")
    public String showEditAccountForm(@RequestParam String email, Model model, RedirectAttributes redirectAttributes) {
        Optional<AppUser> userOptional = appUserRepository.findByEmail(email);
        
        if (userOptional.isPresent()) {
            model.addAttribute("user", userOptional.get());
            model.addAttribute("isEdit", true);
            return "add_edit_account";
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy tài khoản!");
            return "redirect:/admin/account_management";
        }
    }

    @PostMapping("/edit_account")
    public String editAccount(@ModelAttribute AppUser user, RedirectAttributes redirectAttributes) {
        try {
            Optional<AppUser> existingUserOptional = appUserRepository.findByEmail(user.getEmail());
            
            if (existingUserOptional.isPresent()) {
                AppUser existingUser = existingUserOptional.get();
                
                // Cập nhật thông tin
                existingUser.setFullName(user.getFullName());
                existingUser.setPhone(user.getPhone());
                existingUser.setAddress(user.getAddress());
                existingUser.setRole(user.getRole());
                existingUser.setBirthDate(user.getBirthDate());
                
                // Chỉ cập nhật username nếu có thay đổi và không trùng với user khác
                if (user.getUsername() != null && !user.getUsername().equals(existingUser.getUsername())) {
                    if (appUserRepository.existsByUsername(user.getUsername())) {
                        redirectAttributes.addFlashAttribute("errorMessage", "Username đã tồn tại!");
                        return "redirect:/admin/edit_account?email=" + user.getEmail();
                    }
                    existingUser.setUsername(user.getUsername());
                }
                
                // Chỉ cập nhật password nếu có nhập password mới
                if (user.getPassword() != null && !user.getPassword().isEmpty()) {
                    existingUser.setPassword(passwordEncoder.encode(user.getPassword()));
                }
                
                appUserRepository.save(existingUser);
                redirectAttributes.addFlashAttribute("successMessage", "Cập nhật tài khoản thành công!");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy tài khoản!");
            }
            
            return "redirect:/admin/account_management";
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra: " + e.getMessage());
            return "redirect:/admin/edit_account?email=" + user.getEmail();
        }
    }

    @PostMapping("/delete_account")
    public String deleteAccount(@RequestParam String email, RedirectAttributes redirectAttributes) {
        try {
            Optional<AppUser> userOptional = appUserRepository.findByEmail(email);
            
            if (userOptional.isPresent()) {
                appUserRepository.delete(userOptional.get());
                redirectAttributes.addFlashAttribute("successMessage", "Xóa tài khoản thành công!");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy tài khoản!");
            }
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra: " + e.getMessage());
        }
        
        return "redirect:/admin/account_management";
    }
}