package com.bmt.MyApp.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.bmt.MyApp.models.ServicePack;

public interface ServicePackRepository extends JpaRepository<ServicePack, Long> {

    // Tìm kiếm theo tên hoặc mô tả (không phân biệt hoa thường)
    Page<ServicePack> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
        String name, String description, Pageable pageable
    );
}
