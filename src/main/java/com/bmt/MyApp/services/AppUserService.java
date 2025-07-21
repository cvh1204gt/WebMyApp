package com.bmt.MyApp.services;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.bmt.MyApp.models.AppUser;
import com.bmt.MyApp.repositories.AppUserRepository;

/**
 * Service for user management and authentication logic.
 */
@Service
public class AppUserService implements UserDetailsService {

    @Autowired
    private AppUserRepository repo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Loads a user by email for authentication.
     * @param email the user's email
     * @return UserDetails for Spring Security
     * @throws UsernameNotFoundException if user not found or not verified
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        AppUser appUser = repo.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
        if (!appUser.isVerified()) {
            throw new UsernameNotFoundException("Account not verified. Please check your email for OTP verification.");
        }
        return User.withUsername(appUser.getEmail())
                .password(appUser.getPassword())
                .roles(appUser.getRole().toUpperCase())
                .build();
    }

    /**
     * Gets the currently authenticated user.
     * @return the current AppUser or null if not authenticated
     */
    public AppUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            String email = authentication.getName();
            return repo.findByEmail(email).orElse(null);
        }
        return null;
    }

    /**
     * Gets a user by ID.
     * @param id the user ID
     * @return Optional of AppUser
     */
    public Optional<AppUser> getUserById(Integer id) {
        return repo.findById(id);
    }

    /**
     * Gets a user by email.
     * @param email the user's email
     * @return Optional of AppUser
     */
    public Optional<AppUser> getUserByEmail(String email) {
        return repo.findByEmail(email);
    }

    /**
     * Updates a user entity.
     * @param user the user to update
     * @return the updated AppUser
     */
    public AppUser updateUser(AppUser user) {
        return repo.save(user);
    }

    /**
     * Updates the profile of the currently authenticated user.
     * @param updatedUser the new user data
     * @return the updated AppUser or null if not authenticated
     */
    public AppUser updateCurrentUserProfile(AppUser updatedUser) {
        AppUser currentUser = getCurrentUser();
        if (currentUser != null) {
            currentUser.setFullName(updatedUser.getFullName());
            currentUser.setPhone(updatedUser.getPhone());
            currentUser.setAddress(updatedUser.getAddress());
            currentUser.setBirthDate(updatedUser.getBirthDate());
            if (updatedUser.getPassword() != null && !updatedUser.getPassword().isEmpty()) {
                currentUser.setPassword(passwordEncoder.encode(updatedUser.getPassword()));
            }
            return repo.save(currentUser);
        }
        return null;
    }

    /**
     * Checks if an email already exists.
     * @param email the email to check
     * @return true if exists, false otherwise
     */
    public boolean existsByEmail(String email) {
        return repo.existsByEmail(email);
    }

    /**
     * Checks if a username already exists.
     * @param username the username to check
     * @return true if exists, false otherwise
     */
    public boolean existsByUsername(String username) {
        return repo.existsByUsername(username);
    }
}