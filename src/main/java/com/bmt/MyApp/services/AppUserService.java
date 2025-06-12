package com.bmt.MyApp.services;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;

import com.bmt.MyApp.models.AppUser;
import com.bmt.MyApp.repositories.AppUserRepository;

@Service
public class AppUserService implements UserDetailsService {

    @Autowired
    private AppUserRepository repo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        AppUser appUser = repo.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        // Kiểm tra xem user đã được xác thực chưa
        if (!appUser.isVerified()) {
            throw new UsernameNotFoundException("Account not verified. Please check your email for OTP verification.");
        }

        return User.withUsername(appUser.getEmail())
                .password(appUser.getPassword())
                .roles(appUser.getRole().toUpperCase())
                .build();
    }

    // Lấy thông tin user hiện tại đang đăng nhập
    public AppUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            String email = authentication.getName();
            return repo.findByEmail(email).orElse(null);
        }
        return null;
    }

    // Lấy user theo ID
    public Optional<AppUser> getUserById(Integer id) {
        return repo.findById(id);
    }

    // Lấy user theo email
    public Optional<AppUser> getUserByEmail(String email) {
        return repo.findByEmail(email);
    }

    // Cập nhật thông tin user
    public AppUser updateUser(AppUser user) {
        // Nếu AppUser có trường updatedAt
        // user.setUpdatedAt(LocalDate.now());
        return repo.save(user);
    }

    public AppUser updateCurrentUserProfile(AppUser updatedUser) {
        AppUser currentUser = getCurrentUser();
        if (currentUser != null) {
            // Cập nhật các thông tin được phép thay đổi
            // Điều chỉnh theo các field thực tế trong AppUser entity của bạn
            currentUser.setFullName(updatedUser.getFullName());
            currentUser.setPhone(updatedUser.getPhone());
            currentUser.setAddress(updatedUser.getAddress());
            currentUser.setBirthDate(updatedUser.getBirthDate());

            // Chỉ cập nhật password nếu có nhập password mới
            if (updatedUser.getPassword() != null && !updatedUser.getPassword().isEmpty()) {
                currentUser.setPassword(passwordEncoder.encode(updatedUser.getPassword()));
            }

            // Nếu có trường updatedAt
            // currentUser.setUpdatedAt(LocalDate.now());
            return repo.save(currentUser);
        }
        return null;
    }

    // Kiểm tra email đã tồn tại chưa
    public boolean existsByEmail(String email) {
        return repo.existsByEmail(email);
    }

    // Kiểm tra username đã tồn tại chưa
    public boolean existsByUsername(String username) {
        return repo.existsByUsername(username);
    }
}