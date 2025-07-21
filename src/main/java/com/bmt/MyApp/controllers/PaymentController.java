package com.bmt.MyApp.controllers;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.bmt.MyApp.config.VNPayConfig;
import com.bmt.MyApp.models.AppUser;
import com.bmt.MyApp.models.ServicePack;
import com.bmt.MyApp.models.ServicePackMember;
import com.bmt.MyApp.models.Services;
import com.bmt.MyApp.models.Transactions;
import com.bmt.MyApp.models.Transactions.TransactionStatus;
import com.bmt.MyApp.repositories.AppUserRepository;
import com.bmt.MyApp.repositories.ServicePackMemberRepository;
import com.bmt.MyApp.repositories.ServicePackRepository;
import com.bmt.MyApp.repositories.ServicesRepository;
import com.bmt.MyApp.repositories.TransactionsRepository;
import com.bmt.MyApp.services.LogService;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Controller for handling payment creation and VNPay return logic.
 */
@Controller
@RequestMapping("/api/payment")
public class PaymentController {

    @Autowired private AppUserRepository appUserRepository;
    @Autowired private TransactionsRepository transactionsRepository;
    @Autowired private ServicesRepository servicesRepository;
    @Autowired private LogService logService;
    @Autowired private ServicePackRepository servicePackRepository;
    @Autowired private ServicePackMemberRepository servicePackMemberRepository;

    /**
     * Initiates a payment for a service using VNPay.
     * @param serviceId the ID of the service
     * @param amount the amount to pay
     * @param request the HTTP request
     * @param redirectAttributes redirect attributes for error messages
     * @return redirect to VNPay payment URL or error page
     */
    @GetMapping("/create_payment_service")
    public String createPaymentForService(
            @RequestParam("id") Integer serviceId,
            @RequestParam("amount") BigDecimal amount,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String email = auth.getName();
            AppUser currentUser = appUserRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

            Optional<Transactions> activeTransaction = transactionsRepository
                .findActiveTransactionByUserAndService(currentUser, serviceId.longValue(), LocalDateTime.now());

            if (activeTransaction.isPresent()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Bạn đã mua gói dịch vụ này và vẫn còn hiệu lực!");
                return "redirect:/services";
            }

            long amountInVND = amount.multiply(BigDecimal.valueOf(100)).longValue();
            String vnp_TxnRef = VNPayConfig.getRandomNumber(8);
            String vnp_IpAddr = VNPayConfig.getIpAddress(request);

            Map<String, String> vnp_Params = new HashMap<>();
            vnp_Params.put("vnp_Version", VNPayConfig.vnp_Version);
            vnp_Params.put("vnp_Command", VNPayConfig.vnp_Command);
            vnp_Params.put("vnp_TmnCode", VNPayConfig.vnp_TmnCode);
            vnp_Params.put("vnp_Amount", String.valueOf(amountInVND));
            vnp_Params.put("vnp_CurrCode", "VND");
            vnp_Params.put("vnp_BankCode", "NCB");
            vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
            vnp_Params.put("vnp_OrderInfo", "Thanh toán dịch vụ ID: " + serviceId);
            vnp_Params.put("vnp_OrderType", VNPayConfig.orderType);
            vnp_Params.put("vnp_Locale", "vn");
            vnp_Params.put("vnp_ReturnUrl", VNPayConfig.vnp_ReturnUrl);
            vnp_Params.put("vnp_IpAddr", vnp_IpAddr);

            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
            Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh")); // ĐÚNG MÚI GIỜ VIỆT NAM
            String createDate = formatter.format(cld.getTime());
            cld.add(Calendar.MINUTE, 15);
            String expireDate = formatter.format(cld.getTime());

            // Thêm log kiểm tra thời gian thực tế và thời gian gửi lên VNPay
            System.out.println("System current time: " + new java.util.Date());
            System.out.println("VNPay createDate: " + createDate);
            System.out.println("VNPay expireDate: " + expireDate);

            vnp_Params.put("vnp_CreateDate", createDate);
            vnp_Params.put("vnp_ExpireDate", expireDate);

            List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
            Collections.sort(fieldNames);

            StringBuilder hashData = new StringBuilder();
            StringBuilder query = new StringBuilder();

            for (int i = 0; i < fieldNames.size(); i++) {
                String name = fieldNames.get(i);
                String value = URLEncoder.encode(vnp_Params.get(name), StandardCharsets.UTF_8);
                hashData.append(name).append("=").append(value);
                query.append(URLEncoder.encode(name, StandardCharsets.UTF_8)).append("=").append(value);
                if (i < fieldNames.size() - 1) {
                    hashData.append("&");
                    query.append("&");
                }
            }

            String vnp_SecureHash = VNPayConfig.hmacSHA512(VNPayConfig.secretKey, hashData.toString());
            query.append("&vnp_SecureHash=").append(vnp_SecureHash);

            String paymentUrl = VNPayConfig.vnp_PayUrl + "?" + query.toString();
            return "redirect:" + paymentUrl;

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra khi tạo thanh toán: " + e.getMessage());
            return "redirect:/services";
        }
    }

    /**
     * Handles the VNPay return callback and updates user/service/transaction accordingly.
     * @param allParams all VNPay return parameters
     * @param request the HTTP request
     * @param model the Spring MVC model
     * @return receipt or error view
     */
    @GetMapping("/vnpay-return")
    public String vnpayReturn(@RequestParam Map<String, String> allParams,
                              HttpServletRequest request,
                              Model model) {

        String vnp_ResponseCode = allParams.get("vnp_ResponseCode");

        if ("00".equals(vnp_ResponseCode)) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String email = auth.getName();
            AppUser user = appUserRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

            String orderInfo = allParams.get("vnp_OrderInfo");
            Long serviceId = Long.parseLong(orderInfo.replaceAll("\\D+", ""));
            Services service = servicesRepository.findById(serviceId)
                .orElseThrow(() -> new RuntimeException("Service not found"));

            Optional<Transactions> existingTransaction = transactionsRepository
                .findActiveTransactionByUserAndService(user, serviceId, LocalDateTime.now());

            if (existingTransaction.isPresent()) {
                model.addAttribute("error", "Bạn đã có gói dịch vụ này đang hoạt động!");
                return "error";
            }

            user.setRole("ADMINDICHVU");
            appUserRepository.save(user);

            // Cập nhật lại SecurityContextHolder để session có quyền mới ngay lập tức
            List<org.springframework.security.core.authority.SimpleGrantedAuthority> updatedAuthorities =
                java.util.Collections.singletonList(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMINDICHVU"));
            org.springframework.security.core.userdetails.UserDetails updatedUserDetails = org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())
                .authorities(updatedAuthorities)
                .build();
            org.springframework.security.authentication.UsernamePasswordAuthenticationToken newAuth =
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                    updatedUserDetails, null, updatedAuthorities);
            org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(newAuth);

            // Thêm vào ServicePackMember nếu chưa có
            ServicePack servicePack = servicePackRepository.findById(serviceId).orElse(null);
            if (servicePack != null && !servicePackMemberRepository.existsByUserAndServicePack(user, servicePack)) {
                ServicePackMember member = new ServicePackMember(user, servicePack);
                servicePackMemberRepository.save(member);
            }

            Transactions transaction = new Transactions();
            transaction.setUser(user);
            transaction.setService(service);
            transaction.setAmount(new BigDecimal(allParams.get("vnp_Amount")).divide(BigDecimal.valueOf(100)));
            transaction.setOrderInfo(orderInfo);
            transaction.setVnpTxnRef(allParams.get("vnp_TxnRef"));
            transaction.setVnpTransactionNo(allParams.get("vnp_TransactionNo"));
            transaction.setVnpResponseCode(vnp_ResponseCode);
            transaction.setVnpPayDate(allParams.get("vnp_PayDate"));
            transaction.setBankCode(allParams.get("vnp_BankCode"));
            transaction.setLocale(allParams.get("vnp_Locale"));
            transaction.setCurrencyCode("VND");
            transaction.setStatus(TransactionStatus.SUCCESS);
            transaction.setCreatedAt(LocalDateTime.now());
            transaction.setExpireDate(LocalDateTime.now().plusDays(service.getDuration()));

            transactionsRepository.save(transaction);

            // ✅ GHI LOG MUA DỊCH VỤ
            String detail = String.format("Mua gói dịch vụ '%s' trị giá %.0f VND, thời hạn %d ngày.",
                service.getName(),
                transaction.getAmount().doubleValue(),
                service.getDuration());

            logService.log(user.getEmail(), "Mua dịch vụ", detail);

            model.addAttribute("amount", transaction.getAmount());
            model.addAttribute("bankcode", transaction.getBankCode());
            model.addAttribute("orderinfo", orderInfo);
            model.addAttribute("expireDate", transaction.getExpireDate());
            return "receipt";

        } else {
            model.addAttribute("error", "payment_failed");
            return "error";
        }
    }
}
