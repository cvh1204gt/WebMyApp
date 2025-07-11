package com.bmt.MyApp.services;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.bmt.MyApp.models.ServicePack;
import com.bmt.MyApp.models.SystemLog;
import com.bmt.MyApp.repositories.ServicePackRepository;
import com.bmt.MyApp.repositories.SystemLogRepository;

@Service
public class ServicePackService {

    @Autowired
    private ServicePackRepository serviceRepo;

    @Autowired
    private SystemLogRepository logRepo;

    // Tìm kiếm + phân trang
    public Page<ServicePack> searchPaged(String search, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        if (search == null || search.isBlank()) {
            return serviceRepo.findAll(pageable);
        }
        return serviceRepo.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(search, search, pageable);
    }

    public void add(ServicePack s, String username) {
        s.setCreatedAt(LocalDateTime.now());
        s.setUpdatedAt(LocalDateTime.now());
        serviceRepo.save(s);
        logRepo.save(SystemLog.builder()
                .username(username)
                .action("Thêm")
                .detail("Thêm gói: " + s.getName())
                .timestamp(LocalDateTime.now())
                .build());
    }

    public void update(Long id, ServicePack s, String username) {
        ServicePack existing = serviceRepo.findById(id).orElseThrow();
        existing.setName(s.getName());
        existing.setPrice(s.getPrice());
        existing.setDescription(s.getDescription());
        existing.setDuration(s.getDuration());
        existing.setUpdatedAt(LocalDateTime.now());
        serviceRepo.save(existing);
        logRepo.save(SystemLog.builder()
                .username(username)
                .action("Sửa")
                .detail("Sửa gói: " + existing.getName())
                .timestamp(LocalDateTime.now())
                .build());
    }

    public void delete(Long id, String username) {
        ServicePack s = serviceRepo.findById(id).orElseThrow();
        serviceRepo.delete(s);
        logRepo.save(SystemLog.builder()
                .username(username)
                .action("Xóa")
                .detail("Xóa gói: " + s.getName())
                .timestamp(LocalDateTime.now())
                .build());
    }

    public ServicePack findById(Long id) {
        return serviceRepo.findById(id).orElseThrow();
    }
}
