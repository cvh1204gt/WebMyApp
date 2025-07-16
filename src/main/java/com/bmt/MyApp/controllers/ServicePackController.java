package com.bmt.MyApp.controllers;

import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.bmt.MyApp.models.AppUser;
import com.bmt.MyApp.models.ServicePack;
import com.bmt.MyApp.repositories.AppUserRepository;
import com.bmt.MyApp.services.ServicePackService;

@Controller
@RequestMapping("/servicepacks")
public class ServicePackController {

    @Autowired
    private ServicePackService service;

    @Autowired
    private AppUserRepository appUserRepository;

    // Danh sách + tìm kiếm + phân trang
    @GetMapping
    public String listPaged(Model model,
                            @RequestParam(defaultValue = "") String search,
                            @RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "10") int size) {

        Page<ServicePack> paged = service.searchPaged(search, page, size);
        model.addAttribute("packs", paged.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", paged.getTotalPages());
        model.addAttribute("totalElements", paged.getTotalElements());
        model.addAttribute("size", size);
        model.addAttribute("hasNext", paged.hasNext());
        model.addAttribute("hasPrevious", paged.hasPrevious());
        model.addAttribute("search", search);

        return "servicepack";
    }

    // Hiển thị form thêm
    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("pack", new ServicePack());
        return "addservicepack";
    }

    // Xử lý thêm mới
    @PostMapping("/add")
    public String add(@ModelAttribute ServicePack pack, Principal principal, RedirectAttributes redirectAttributes) {
        try {
            service.add(pack, principal.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Thêm gói dịch vụ thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi thêm gói dịch vụ!");
        }
        
        return "redirect:/servicepacks";
    }

    // Hiển thị form sửa
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        model.addAttribute("pack", service.findById(id));
        return "editservicepack";
    }

    // Xử lý cập nhật
    @PostMapping("/edit/{id}")
    public String edit(@PathVariable Long id, @ModelAttribute ServicePack pack, Principal principal, RedirectAttributes redirectAttributes) {
        try {
            service.update(id, pack, principal.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật gói dịch vụ thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi cập nhật gói dịch vụ!");
        }
        return "redirect:/servicepacks";
    }

    // Xử lý xóa
    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id, Principal principal, RedirectAttributes redirectAttributes) {
        try {
            service.delete(id, principal.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Xóa gói dịch vụ thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi xóa gói dịch vụ!");
        }
        
        return "redirect:/servicepacks";
    }








    // Trang thêm thành viên (form đơn)
    @GetMapping("/add-member-page")
    public String showAddMemberPage(@RequestParam(value = "servicePackId", required = false) Long servicePackId, Model model, Principal principal, RedirectAttributes redirectAttributes) {
        String email = principal.getName();
        var packs = service.getPurchasedServicePacksOfUser(email);
        if (packs == null || packs.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn chưa mua gói dịch vụ nào để thêm thành viên!");
            return "redirect:/servicepacks/members";
        }
        model.addAttribute("packs", packs);
        model.addAttribute("selectedPackId", servicePackId);
        return "add_member";
    }

    // FIX: Thêm thành viên vào gói dịch vụ - Đảm bảo redirect đúng servicePackId
    @PostMapping("/add-member")
    public String addMemberToServicePack(@RequestParam Long servicePackId,
                                         @RequestParam String userEmail,
                                         Principal principal,
                                         RedirectAttributes redirectAttributes) {
        // Kiểm tra servicePackId hợp lệ
        if (servicePackId == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng chọn gói dịch vụ!");
            return "redirect:/servicepacks/add-member-page";
        }

        // Kiểm tra servicePack có tồn tại không
        try {
            ServicePack servicePack = service.findById(servicePackId);
            if (servicePack == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Gói dịch vụ không tồn tại!");
                return "redirect:/servicepacks/add-member-page";
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Gói dịch vụ không tồn tại!");
            return "redirect:/servicepacks/add-member-page";
        }

        // Lấy user hiện tại từ DB
        AppUser currentUser = appUserRepository.findByEmail(principal.getName()).orElse(null);
        if (currentUser == null || !"ADMINDICHVU".equals(currentUser.getRole())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Chỉ ADMINDICHVU mới được phép thêm thành viên vào gói dịch vụ!");
            return "redirect:/servicepacks/members?servicePackId=" + servicePackId;
        }

        String result = service.addMember(servicePackId, userEmail, principal.getName());
        if ("success".equals(result)) {
            redirectAttributes.addFlashAttribute("successMessage", "Thêm thành viên thành công!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", result);
        }
        
        // FIX: Đảm bảo redirect về đúng servicePackId
        return "redirect:/servicepacks/members?servicePackId=" + servicePackId;
    }

    // Trang tổng hợp các gói dịch vụ (bảng), hoặc xem thành viên của 1 gói
    @GetMapping("/members")
    public String showServicePackMembers(@RequestParam(value = "servicePackId", required = false) Long servicePackId,
                                         @RequestParam(defaultValue = "") String search,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "10") int size,
                                         Model model, Principal principal) {
        if (servicePackId == null) {
            // Hiển thị bảng tổng hợp các gói mà user đã mua
            String email = principal.getName();
            var packs = service.getPurchasedServicePacksOfUser(email);
            // Nếu có search, filter theo tên gói
            if (search != null && !search.isBlank()) {
                packs = packs.stream()
                        .filter(p -> p.getName() != null && p.getName().toLowerCase().contains(search.toLowerCase()))
                        .toList();
            }
            for (var pack : packs) {
                pack.setMembers(service.getMembersByServicePack(pack.getId()));
            }
            model.addAttribute("packs", packs);
            model.addAttribute("search", search);
            return "service_pack_members";
        }
        // Nếu có servicePackId, hiển thị danh sách thành viên của gói đó
        try {
            ServicePack servicePack = service.findById(servicePackId);
            if (servicePack == null) {
                return "redirect:/servicepacks/members";
            }
            model.addAttribute("servicePack", servicePack);
            var memberPage = service.searchMembersByServicePack(servicePackId, search, page, size);
            model.addAttribute("members", memberPage.getContent());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", memberPage.getTotalPages());
            model.addAttribute("totalElements", memberPage.getTotalElements());
            model.addAttribute("hasNext", memberPage.hasNext());
            model.addAttribute("hasPrevious", memberPage.hasPrevious());
            model.addAttribute("size", size);
            model.addAttribute("search", search);
        } catch (Exception e) {
            return "redirect:/servicepacks/members";
        }
        return "service_pack_members";
    }

    // FIX: Thêm method xử lý xóa thành viên
    @PostMapping("/members/remove/{memberId}")
    public String removeMember(@PathVariable Long memberId, 
                               Principal principal,
                               RedirectAttributes redirectAttributes) {
        // Lấy thông tin member trước khi xóa để có servicePackId
        try {
            var member = service.findMemberById(memberId);
            if (member == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Thành viên không tồn tại!");
                return "redirect:/servicepacks";
            }
            
            Long servicePackId = member.getServicePack().getId();
            String result = service.removeMember(memberId, principal.getName());
            
            if ("success".equals(result)) {
                redirectAttributes.addFlashAttribute("successMessage", "Xóa thành viên thành công!");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", result);
            }
            
            return "redirect:/servicepacks/members?servicePackId=" + servicePackId;
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra khi xóa thành viên!");
            return "redirect:/servicepacks";
        }
    }
}