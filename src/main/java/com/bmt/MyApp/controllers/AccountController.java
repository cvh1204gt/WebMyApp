package com.bmt.MyApp.controllers;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.bmt.MyApp.models.AppUser;
import com.bmt.MyApp.models.Services;
import com.bmt.MyApp.repositories.AppUserRepository;
import com.bmt.MyApp.repositories.ServicesRepository;
import com.bmt.MyApp.services.LogService;

/**
 * Controller for admin account and service management.
 */
@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AccountController {

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private LogService logService;

    @Autowired
    private ServicesRepository servicesRepository;

    /**
     * Displays the account management page with search and pagination.
     */
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

    /**
     * Displays the add account form.
     */
    @GetMapping("/add_account")
    public String showAddAccountForm(Model model) {
        model.addAttribute("user", new AppUser());
        model.addAttribute("isEdit", false);
        return "add_edit_account";
    }

    /**
     * Handles add account form submission.
     */
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

    /**
     * Displays the edit account form.
     */
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

    /**
     * Handles edit account form submission.
     */
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

    /**
     * Handles account deletion.
     */
    @PostMapping("/delete_account")
    public String deleteAccount(@RequestParam String email, RedirectAttributes redirectAttributes) {
        try {
            // Lấy thông tin người dùng hiện tại đang thực hiện xóa
            String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
            
            // Kiểm tra không cho phép xóa tài khoản của chính mình
            if (currentUserEmail.equals(email)) {
                redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa tài khoản của chính mình!");
                return "redirect:/admin/account_management";
            }
            
            Optional<AppUser> userOptional = appUserRepository.findByEmail(email);
            
            if (userOptional.isPresent()) {
                AppUser userToDelete = userOptional.get();
                String userFullName = userToDelete.getFullName();
                String userEmail = userToDelete.getEmail();
                
                // Xóa user (cascade sẽ tự động xóa các transactions liên quan)
                appUserRepository.delete(userToDelete);
                
                // Log hành động xóa tài khoản với email của người thực hiện
                logService.log(currentUserEmail, "Xóa tài khoản", 
                    "Đã xóa tài khoản: " + userFullName + " (" + userEmail + ") và tất cả giao dịch liên quan");
                
                redirectAttributes.addFlashAttribute("successMessage", "Xóa tài khoản thành công!");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy tài khoản!");
            }
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra: " + e.getMessage());
        }
        
        return "redirect:/admin/account_management";
    }

    /**
     * Displays the service management page with search, filter, and pagination.
     */
    @GetMapping("/service_management")
    public String serviceManagement(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "") String startDate,
            @RequestParam(defaultValue = "") String endDate,
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(defaultValue = "desc") String direction,
            Model model) {
        
        Pageable pageable = PageRequest.of(page, 10);
        Page<Services> servicePage;
        
        if (search.isEmpty()) {
            servicePage = servicesRepository.findAll(pageable);
        } else {
            // Tìm kiếm theo tên dịch vụ
            servicePage = servicesRepository.findByNameContainingIgnoreCase(search, pageable);
        }
        
        model.addAttribute("services", servicePage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", servicePage.getTotalPages());
        model.addAttribute("totalElements", servicePage.getTotalElements());
        model.addAttribute("hasPrevious", servicePage.hasPrevious());
        model.addAttribute("hasNext", servicePage.hasNext());
        model.addAttribute("search", search);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("sort", sort);
        model.addAttribute("direction", direction);
        
        return "service_management";
    }

    /**
     * Displays the add service form.
     */
    @GetMapping("/add_service")
    public String showAddServiceForm(Model model) {
        model.addAttribute("service", new Services());
        model.addAttribute("isEdit", false);
        return "add_edit_service";
    }

    /**
     * Handles add service form submission.
     */
    @PostMapping("/add_service")
    public String addService(@ModelAttribute Services service, RedirectAttributes redirectAttributes) {
        try {
            service.setCreatedAt(LocalDateTime.now());
            servicesRepository.save(service);
            
            // Lấy thông tin người dùng hiện tại
            String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
            
            // Log hành động thêm dịch vụ
            logService.log(currentUserEmail, "Thêm dịch vụ", 
                "Đã thêm dịch vụ: " + service.getName() + " (ID: " + service.getId() + ")");
            
            redirectAttributes.addFlashAttribute("successMessage", "Thêm dịch vụ thành công!");
            return "redirect:/admin/service_management";
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra: " + e.getMessage());
            return "redirect:/admin/add_service";
        }
    }

    /**
     * Displays the edit service form.
     */
    @GetMapping("/edit_service/{id}")
    public String showEditServiceForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Optional<Services> serviceOptional = servicesRepository.findById(id);
        
        if (serviceOptional.isPresent()) {
            model.addAttribute("service", serviceOptional.get());
            model.addAttribute("isEdit", true);
            return "add_edit_service";
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy dịch vụ!");
            return "redirect:/admin/service_management";
        }
    }

    /**
     * Handles edit service form submission.
     */
    @PostMapping("/edit_service/{id}")
    public String editService(@PathVariable Long id, @ModelAttribute Services service, RedirectAttributes redirectAttributes) {
        try {
            Optional<Services> existingServiceOptional = servicesRepository.findById(id);
            
            if (existingServiceOptional.isPresent()) {
                Services existingService = existingServiceOptional.get();
                
                // Cập nhật thông tin
                existingService.setName(service.getName());
                existingService.setPrice(service.getPrice());
                existingService.setDuration(service.getDuration());
                existingService.setDescription(service.getDescription());
                
                servicesRepository.save(existingService);
                
                // Lấy thông tin người dùng hiện tại
                String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
                
                // Log hành động sửa dịch vụ
                logService.log(currentUserEmail, "Sửa dịch vụ", 
                    "Đã sửa dịch vụ: " + existingService.getName() + " (ID: " + existingService.getId() + ")");
                
                redirectAttributes.addFlashAttribute("successMessage", "Cập nhật dịch vụ thành công!");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy dịch vụ!");
            }
            
            return "redirect:/admin/service_management";
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra: " + e.getMessage());
            return "redirect:/admin/edit_service/" + id;
        }
    }

    /**
     * Handles service deletion.
     */
    @PostMapping("/delete_service/{id}")
    public String deleteService(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Optional<Services> serviceOptional = servicesRepository.findById(id);
            
            if (serviceOptional.isPresent()) {
                Services serviceToDelete = serviceOptional.get();
                String serviceName = serviceToDelete.getName();
                Long serviceId = serviceToDelete.getId();
                
                // Xóa service (cascade sẽ tự động xóa các transactions liên quan)
                servicesRepository.delete(serviceToDelete);
                
                // Lấy thông tin người dùng hiện tại
                String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
                
                // Log hành động xóa dịch vụ
                logService.log(currentUserEmail, "Xóa dịch vụ", 
                    "Đã xóa dịch vụ: " + serviceName + " (ID: " + serviceId + ") và tất cả giao dịch liên quan");
                
                redirectAttributes.addFlashAttribute("successMessage", "Xóa dịch vụ thành công!");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy dịch vụ!");
            }
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra: " + e.getMessage());
        }
        
        return "redirect:/admin/service_management";
    }
}