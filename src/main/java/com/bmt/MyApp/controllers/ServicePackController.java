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

import com.bmt.MyApp.models.ServicePack;
import com.bmt.MyApp.services.ServicePackService;

@Controller
@RequestMapping("/servicepacks")
public class ServicePackController {

    @Autowired
    private ServicePackService service;

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
    public String add(@ModelAttribute ServicePack pack, Principal principal) {
        service.add(pack, principal.getName());
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
    public String edit(@PathVariable Long id, @ModelAttribute ServicePack pack, Principal principal) {
        service.update(id, pack, principal.getName());
        return "redirect:/servicepacks";
    }

    // Xử lý xóa
    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id, Principal principal) {
        service.delete(id, principal.getName());
        return "redirect:/servicepacks";
    }
}
