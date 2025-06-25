package com.bmt.MyApp.repositories;

import com.bmt.MyApp.models.Services;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ServicesRepository extends JpaRepository<Services, Long> {
  
  // Optional<Services> findById(Integer Id);
}
