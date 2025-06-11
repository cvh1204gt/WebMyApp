package com.bmt.MyApp.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.bmt.MyApp.repositories.AppUserRepository;

@Service
public class AppUserService implements UserDetailsService {
  @Autowired
  private AppUserRepository repo;

  @Override
  public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    return repo.findByEmail(email)
        .map(appUser -> User.withUsername(appUser.getEmail())
                            .password(appUser.getPassword())
                            .roles(appUser.getRole().toUpperCase())
                            .build())
        .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
}
  
}
