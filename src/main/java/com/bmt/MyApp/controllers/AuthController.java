package com.bmt.MyApp.controllers;

import java.util.Date;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.bmt.MyApp.models.AppUser;
import com.bmt.MyApp.models.OtpVerificationDto;
import com.bmt.MyApp.models.RegisterDto;
import com.bmt.MyApp.repositories.AppUserRepository;
import com.bmt.MyApp.repositories.PasswordResetTokenRepository;
import com.bmt.MyApp.services.EmailService;
import com.bmt.MyApp.services.OtpService;

import jakarta.validation.Valid;

/**
 * Controller for authentication, registration, OTP verification, and password reset.
 */
@Controller
public class AuthController {
    
    @Autowired
    private AppUserRepository repo;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private OtpService otpService;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;
    @Autowired
    private EmailService emailService;

    /**
     * Displays the login page.
     */
    @GetMapping("/login")
    public String login() {
        return "login";
    }

    /**
     * Displays the registration page.
     */
    @GetMapping("/register")
    public String register(Model model) {
        RegisterDto registerDto = new RegisterDto();
        model.addAttribute("registerDto", registerDto);
        model.addAttribute("success", false);
        return "register";
    }

    /**
     * Handles user registration form submission.
     */
    @PostMapping("/register")
    public String register(Model model, @Valid @ModelAttribute RegisterDto registerDto, 
                          BindingResult result, RedirectAttributes redirectAttributes) {

        // Validate password confirmation
        if(!registerDto.getPassword().equals(registerDto.getConfirmPassword())) {
            result.addError(
                new FieldError("registerDto", "confirmPassword", "Nhập lại mật khẩu không chính xác, mời nhập lại!")
            );
        }

        // Check if email already exists
        Optional<AppUser> existingUser = repo.findByEmail(registerDto.getEmail());
        if(existingUser.isPresent()) {
            result.addError(
                new FieldError("registerDto", "email", "Email đã tồn tại, mời nhập lại!")
            );
        }

        if(result.hasErrors()) {
            return "register";
        }

        try {
            // Create new user
            AppUser newUser = new AppUser();
            newUser.setFullName(registerDto.getFullName());
            newUser.setEmail(registerDto.getEmail());
            newUser.setPhone(registerDto.getPhone());
            newUser.setAddress(registerDto.getAddress());
            newUser.setRole("CLIENT");
            newUser.setCreateAt(new Date());
            newUser.setPassword(passwordEncoder.encode(registerDto.getPassword()));
            newUser.setVerified(false); // User chưa được xác thực

            repo.save(newUser);

            // Generate and send OTP
            otpService.generateAndSendOtp(registerDto.getEmail());

            // Redirect to OTP verification page
            redirectAttributes.addAttribute("email", registerDto.getEmail());
            redirectAttributes.addFlashAttribute("message", "Đăng ký thành công! Vui lòng kiểm tra email để lấy mã OTP.");
            return "redirect:/verify-otp";

        } catch(Exception ex) {
            result.addError(
                new FieldError("registerDto", "fullName", "Có lỗi xảy ra: " + ex.getMessage())
            );
            return "register";
        }
    }

    /**
     * Displays the OTP verification page.
     */
    @GetMapping("/verify-otp")
    public String showOtpVerification(@RequestParam("email") String email, Model model) {
        OtpVerificationDto otpDto = new OtpVerificationDto();
        otpDto.setEmail(email);
        model.addAttribute("otpVerificationDto", otpDto);
        return "verify-otp";
    }

    /**
     * Handles OTP verification form submission.
     */
    @PostMapping("/verify-otp")
    public String verifyOtp(@Valid @ModelAttribute OtpVerificationDto otpDto, 
                           BindingResult result, Model model, RedirectAttributes redirectAttributes) {
        
        if(result.hasErrors()) {
            return "verify-otp";
        }

        boolean isValid = otpService.verifyOtp(otpDto.getEmail(), otpDto.getOtp());
        
        if(isValid) {
            redirectAttributes.addFlashAttribute("successMessage", "Xác thực thành công! Bạn có thể đăng nhập ngay bây giờ.");
            return "redirect:/login";
        } else {
            model.addAttribute("error", "Mã OTP không hợp lệ hoặc đã hết hạn!");
            return "verify-otp";
        }
    }

    /**
     * Handles resending OTP for registration.
     */
    @PostMapping("/resend-otp")
    public String resendOtp(@RequestParam("email") String email, RedirectAttributes redirectAttributes) {
        try {
            otpService.generateAndSendOtp(email);
            redirectAttributes.addFlashAttribute("message", "Mã OTP mới đã được gửi đến email của bạn!");
        } catch(Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra khi gửi OTP!");
        }
        
        redirectAttributes.addAttribute("email", email);
        return "redirect:/verify-otp";
    }

    /**
     * Displays the forgot password form.
     */
    @GetMapping("/forgot-password")
    public String forgotPasswordForm() {
        return "forgot_password";
    }

    /**
     * Handles forgot password form submission.
     */
    @PostMapping("/forgot-password")
    public String handleForgotPassword(@RequestParam("email") String email, RedirectAttributes redirectAttributes) {
        Optional<AppUser> userOpt = repo.findByEmail(email);
        if (userOpt.isPresent()) {
            otpService.generateAndSendOtp(email);
        }
        redirectAttributes.addFlashAttribute("message", "Nếu email tồn tại, mã OTP đã được gửi!");
        redirectAttributes.addAttribute("email", email);
        return "redirect:/reset-password";
    }

    /**
     * Displays the reset password form.
     */
    @GetMapping("/reset-password")
    public String showResetPasswordForm(@RequestParam(value = "email", required = false) String email, Model model) {
        model.addAttribute("email", email);
        return "reset_password";
    }

    /**
     * Handles reset password form submission.
     */
    @PostMapping("/reset-password")
    public String handleResetPassword(@RequestParam("email") String email,
                                      @RequestParam("otp") String otp,
                                      @RequestParam("password") String password,
                                      @RequestParam("confirmPassword") String confirmPassword,
                                      Model model, RedirectAttributes redirectAttributes) {
        if (!password.equals(confirmPassword)) {
            model.addAttribute("email", email);
            model.addAttribute("message", "Mật khẩu nhập lại không khớp.");
            return "reset_password";
        }
        boolean isValid = otpService.verifyOtp(email, otp);
        if (!isValid) {
            model.addAttribute("email", email);
            model.addAttribute("message", "Mã OTP không hợp lệ hoặc đã hết hạn!");
            return "reset_password";
        }
        Optional<AppUser> userOpt = repo.findByEmail(email);
        if (userOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("message", "Không tìm thấy tài khoản.");
            return "redirect:/forgot-password";
        }
        AppUser user = userOpt.get();
        user.setPassword(passwordEncoder.encode(password));
        repo.save(user);
        redirectAttributes.addFlashAttribute("message", "Đặt lại mật khẩu thành công! Bạn có thể đăng nhập.");
        return "redirect:/login";
    }

    /**
     * Handles resending OTP for forgot password.
     */
    @PostMapping("/resend-otp-forgot")
    public String resendOtpForgot(@RequestParam("email") String email, RedirectAttributes redirectAttributes) {
        try {
            otpService.generateAndSendOtp(email);
            redirectAttributes.addFlashAttribute("message", "Mã OTP mới đã được gửi đến email của bạn!");
        } catch(Exception ex) {
            redirectAttributes.addFlashAttribute("message", "Có lỗi xảy ra khi gửi OTP!");
        }
        redirectAttributes.addAttribute("email", email);
        return "redirect:/reset-password";
    }
}