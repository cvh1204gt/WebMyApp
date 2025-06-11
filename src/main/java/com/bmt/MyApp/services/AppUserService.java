package com.bmt.MyApp.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.bmt.MyApp.models.AppUser;
import com.bmt.MyApp.repositories.AppUserRepository;

@Service
public class AppUserService implements UserDetailsService {
    
    @Autowired
    private AppUserRepository repo;

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
}