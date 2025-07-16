package com.bmt.MyApp.services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.bmt.MyApp.models.AppUser;
import com.bmt.MyApp.models.ServicePack;
import com.bmt.MyApp.models.ServicePackMember;
import com.bmt.MyApp.models.Services;
import com.bmt.MyApp.models.SystemLog;
import com.bmt.MyApp.models.Transactions;
import com.bmt.MyApp.repositories.AppUserRepository;
import com.bmt.MyApp.repositories.ServicePackMemberRepository;
import com.bmt.MyApp.repositories.ServicePackRepository;
import com.bmt.MyApp.repositories.SystemLogRepository;
import com.bmt.MyApp.repositories.TransactionsRepository;

@Service
public class ServicePackService {

    @Autowired
    private ServicePackRepository serviceRepo;

    @Autowired
    private SystemLogRepository logRepo;

    @Autowired
    private ServicePackMemberRepository memberRepo;

    @Autowired
    private AppUserRepository userRepo;

    @Autowired
    private TransactionsRepository transactionRepo;

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
        String packName = s.getName();
        Long packId = s.getId();
        
        serviceRepo.delete(s);
        logRepo.save(SystemLog.builder()
                .username(username)
                .action("Xóa")
                .detail("Xóa gói dịch vụ: " + packName + " (ID: " + packId + ")")
                .timestamp(LocalDateTime.now())
                .build());
    }

    public ServicePack findById(Long id) {
        return serviceRepo.findById(id).orElseThrow();
    }

    // Thêm thành viên vào gói dịch vụ
    public String addMember(Long servicePackId, String userEmail, String adminUsername) {
        ServicePack servicePack = serviceRepo.findById(servicePackId).orElse(null);
        if (servicePack == null) return "Gói dịch vụ không tồn tại";
        AppUser user = userRepo.findByEmail(userEmail).orElse(null);
        if (user == null) return "Không tìm thấy người dùng với email này";
        if (memberRepo.existsByUserAndServicePack(user, servicePack)) return "Người dùng đã là thành viên của gói này";
        ServicePackMember member = new ServicePackMember(user, servicePack);
        memberRepo.save(member);
        logRepo.save(SystemLog.builder()
                .username(adminUsername)
                .action("Thêm thành viên")
                .detail("Thêm " + userEmail + " vào gói: " + servicePack.getName())
                .timestamp(LocalDateTime.now())
                .build());
        return "success";
    }

    // Lấy các gói dịch vụ đã mua của user
    public List<Services> getPurchasedServices(AppUser user) {
        System.out.println("DEBUG: Getting purchased services for user: " + user.getEmail());
        List<Services> services = transactionRepo.findPurchasedServicesByUser(user, LocalDateTime.now());
        System.out.println("DEBUG: Found " + services.size() + " purchased services");
        for (Services service : services) {
            System.out.println("DEBUG: Service - ID: " + service.getId() + ", Name: " + service.getName());
        }
        return services;
    }

    // Thêm user thành ADMINDICHVU
    public String addMemberAsAdmin(String userEmail, Long serviceId, String adminUsername) {
        System.out.println("=== DEBUG: Service addMemberAsAdmin ===");
        System.out.println("UserEmail: " + userEmail);
        System.out.println("ServiceId: " + serviceId);
        
        AppUser user = userRepo.findByEmail(userEmail).orElse(null);
        if (user == null) {
            System.out.println("DEBUG: User not found");
            return "Không tìm thấy người dùng với email này";
        }
        System.out.println("DEBUG: User found: " + user.getEmail());
        
        ServicePack servicePack = serviceRepo.findById(serviceId).orElse(null);
        if (servicePack == null) {
            System.out.println("DEBUG: ServicePack not found");
            return "Gói dịch vụ không tồn tại";
        }
        System.out.println("DEBUG: ServicePack found: " + servicePack.getName());
        
        // Kiểm tra user đã là ADMINDICHVU chưa
        if ("ADMINDICHVU".equals(user.getRole())) {
            System.out.println("DEBUG: User already ADMINDICHVU, but will add to service pack");
            // Vẫn cho phép thêm vào gói dịch vụ
        }
        
        // Cập nhật role thành ADMINDICHVU (chỉ nếu chưa phải ADMINDICHVU)
        if (!"ADMINDICHVU".equals(user.getRole())) {
            user.setRole("ADMINDICHVU");
            userRepo.save(user);
            System.out.println("DEBUG: User role updated to ADMINDICHVU");
        } else {
            System.out.println("DEBUG: User already ADMINDICHVU, skipping role update");
        }
        
        // Tạo ServicePackMember
        try {
            ServicePackMember member = new ServicePackMember(user, servicePack);
            memberRepo.save(member);
            System.out.println("DEBUG: ServicePackMember created successfully");
        } catch (Exception e) {
            System.out.println("DEBUG: Error creating ServicePackMember: " + e.getMessage());
            e.printStackTrace();
            return "Lỗi khi tạo thành viên: " + e.getMessage();
        }
        
        logRepo.save(SystemLog.builder()
                .username(adminUsername)
                .action("Thêm ADMINDICHVU")
                .detail("Thêm " + userEmail + " thành ADMINDICHVU với gói: " + servicePack.getName())
                .timestamp(LocalDateTime.now())
                .build());
        System.out.println("DEBUG: Log saved");
        return "success";
    }

    // Thêm method này vào ServicePackService.java

        public ServicePackMember findMemberById(Long memberId) {
            return memberRepo.findById(memberId).orElse(null);
        }

    // Xóa thành viên
    public String removeMember(Long memberId, String adminUsername) {
        ServicePackMember member = memberRepo.findById(memberId).orElse(null);
        if (member == null) return "Thành viên không tồn tại";
        
        String userEmail = member.getUser().getEmail();
        String serviceName = member.getServicePack().getName();
        
        // Xóa ServicePackMember
        memberRepo.delete(member);
        
        // Có thể cập nhật role user về CLIENT nếu cần
        // member.getUser().setRole("CLIENT");
        // userRepo.save(member.getUser());
        
        logRepo.save(SystemLog.builder()
                .username(adminUsername)
                .action("Xóa thành viên")
                .detail("Xóa " + userEmail + " khỏi gói: " + serviceName)
                .timestamp(LocalDateTime.now())
                .build());
        return "success";
    }

    // Lấy danh sách thành viên của admin
    public List<ServicePackMember> getMembersByAdmin(AppUser admin) {
        // TODO: Cần logic để lấy ServicePack của admin
        return new ArrayList<>();
    }

    public List<ServicePackMember> getMembersByServicePack(Long servicePackId) {
        ServicePack servicePack = serviceRepo.findById(servicePackId).orElse(null);
        if (servicePack == null) return new ArrayList<>();
        return memberRepo.findByServicePack(servicePack);
    }

    public List<ServicePack> getServicePacksOfUser(String email) {
        AppUser user = userRepo.findByEmail(email).orElse(null);
        if (user == null) return new ArrayList<>();
        List<ServicePackMember> memberships = memberRepo.findByUser(user);
        List<ServicePack> packs = new ArrayList<>();
        for (ServicePackMember m : memberships) {
            packs.add(m.getServicePack());
        }
        return packs;
    }

    public List<ServicePack> getPurchasedServicePacksOfUser(String email) {
        AppUser user = userRepo.findByEmail(email).orElse(null);
        if (user == null) return new ArrayList<>();
        List<Transactions> transactions = transactionRepo.findByUserAndStatus(user, Transactions.TransactionStatus.SUCCESS);
        Set<Long> packIds = new HashSet<>();
        List<ServicePack> packs = new ArrayList<>();
        for (Transactions t : transactions) {
            if (t.getService() != null && t.getService().getId() != null && !packIds.contains(t.getService().getId())) {
                ServicePack pack = serviceRepo.findById(t.getService().getId()).orElse(null);
                if (pack != null) {
                    packs.add(pack);
                    packIds.add(pack.getId());
                }
            }
        }
        return packs;
    }

    // Tìm kiếm và phân trang thành viên của gói dịch vụ
    public Page<ServicePackMember> searchMembersByServicePack(Long servicePackId, String keyword, int page, int size) {
        ServicePack servicePack = serviceRepo.findById(servicePackId).orElse(null);
        if (servicePack == null) return Page.empty();
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        if (keyword == null || keyword.isBlank()) {
            return memberRepo.findByServicePack(servicePack, pageable);
        } else {
            // Tìm kiếm theo tên, email, số điện thoại
            return memberRepo.findByServicePackAndKeyword(servicePackId, "%" + keyword + "%", pageable);
        }
    }
}
