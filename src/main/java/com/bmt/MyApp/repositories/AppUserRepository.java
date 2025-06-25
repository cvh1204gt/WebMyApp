package com.bmt.MyApp.repositories;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.bmt.MyApp.models.AppUser;

public interface AppUserRepository extends JpaRepository<AppUser, Integer> {

  // public AppUser findByEmail(String email);

  // Optional<User> findByUsername(String username);

  Optional<AppUser> findByEmail(String email);

  boolean existsByUsername(String username);

  boolean existsByEmail(String email);

  // Thêm method để tìm kiếm theo tên và email với phân trang
  Page<AppUser> findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
  String fullName, String email, Pageable pageable);
}
