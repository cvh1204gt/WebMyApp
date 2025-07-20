package com.bmt.MyApp.controllers;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

@Controller
@RequestMapping("/api/payment")
public class PaymentController {

    @Autowired private AppUserRepository appUserRepository;
    @Autowired private TransactionsRepository transactionsRepository;
    @Autowired private ServicesRepository servicesRepository;
    @Autowired private LogService logService;
    @Autowired private ServicePackRepository servicePackRepository;
    @Autowired private ServicePackMemberRepository servicePackMemberRepository;

    @GetMapping("/create_payment_service")
    public String createPaymentForService(
            @RequestParam("id") Integer serviceId,
            @RequestParam("amount") BigDecimal amount,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        try {
            // Log start of payment creation
            System.out.println("=== Starting Payment Creation ===");
            System.out.println("Service ID: " + serviceId);
            System.out.println("Amount: " + amount);

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String email = auth.getName();
            AppUser currentUser = appUserRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

            System.out.println("User: " + currentUser.getEmail());

            Optional<Transactions> activeTransaction = transactionsRepository
                .findActiveTransactionByUserAndService(currentUser, serviceId.longValue(), LocalDateTime.now());

            if (activeTransaction.isPresent()) {
                System.out.println("User already has active transaction for this service");
                redirectAttributes.addFlashAttribute("errorMessage", "Bạn đã mua gói dịch vụ này và vẫn còn hiệu lực!");
                return "redirect:/services";
            }

            // Amount is already in VND, just need to multiply by 100 for VNPay (smallest unit)
            long amountInVND = amount.multiply(BigDecimal.valueOf(100)).longValue();
            String vnp_TxnRef = VNPayConfig.getRandomNumber(8);
            String vnp_IpAddr = VNPayConfig.getIpAddress(request);

            System.out.println("Transaction Ref: " + vnp_TxnRef);
            System.out.println("IP Address: " + vnp_IpAddr);
            System.out.println("Amount in VND (x100): " + amountInVND);

            Map<String, String> vnp_Params = new HashMap<>();
            vnp_Params.put("vnp_Version", VNPayConfig.vnp_Version);
            vnp_Params.put("vnp_Command", VNPayConfig.vnp_Command);
            vnp_Params.put("vnp_TmnCode", VNPayConfig.vnp_TmnCode);
            vnp_Params.put("vnp_Amount", String.valueOf(amountInVND));
            vnp_Params.put("vnp_CurrCode", "VND");
            vnp_Params.put("vnp_BankCode", "NCB");
            vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
            vnp_Params.put("vnp_OrderInfo", "Thanh toan dich vu ID: " + serviceId); // Remove special chars
            vnp_Params.put("vnp_OrderType", VNPayConfig.orderType);
            vnp_Params.put("vnp_Locale", "vn");
            vnp_Params.put("vnp_ReturnUrl", VNPayConfig.vnp_ReturnUrl);
            vnp_Params.put("vnp_IpAddr", vnp_IpAddr);

            // Sử dụng hàm chuẩn từ VNPayConfig để lấy thời gian theo múi giờ Việt Nam
            String createDate = VNPayConfig.getCurrentVNPayDateTime();
            String expireDate = VNPayConfig.getExpireDateTime(30); // 30 phút

            vnp_Params.put("vnp_CreateDate", createDate);
            vnp_Params.put("vnp_ExpireDate", expireDate);

            // Enhanced logging for time
            System.out.println("Create Date: " + createDate);
            System.out.println("Expire Date: " + expireDate);
            System.out.println("Current Vietnam Time: " + new Date());
            System.out.println("Timeout: 30 minutes");

            // Build hash data (raw, không encode) và query string (có encode)
            List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
            Collections.sort(fieldNames);

            // Build hash data (raw value, không encode)
            StringBuilder hashData = new StringBuilder();
            for (int i = 0; i < fieldNames.size(); i++) {
                String fieldName = fieldNames.get(i);
                String fieldValue = vnp_Params.get(fieldName);
                if (fieldValue != null && !fieldValue.isEmpty()) {
                    if (hashData.length() > 0) hashData.append("&");
                    hashData.append(fieldName).append("=").append(fieldValue);
                }
            }
            System.out.println("[VNPay CREATE] Hash data: " + hashData.toString());

            // Build query string (có encode)
            StringBuilder query = new StringBuilder();
            for (int i = 0; i < fieldNames.size(); i++) {
                String fieldName = fieldNames.get(i);
                String fieldValue = vnp_Params.get(fieldName);
                if (fieldValue != null && !fieldValue.isEmpty()) {
                    if (query.length() > 0) query.append("&");
                    query.append(URLEncoder.encode(fieldName, StandardCharsets.UTF_8))
                         .append("=")
                         .append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8));
                }
            }

            // Generate secure hash
            String vnp_SecureHash = VNPayConfig.hmacSHA512(VNPayConfig.secretKey, hashData.toString());
            System.out.println("[VNPay CREATE] Generated hash: " + vnp_SecureHash);
            query.append("&vnp_SecureHash=").append(vnp_SecureHash);

            String paymentUrl = VNPayConfig.vnp_PayUrl + "?" + query.toString();
            
            // Enhanced debugging
            System.out.println("=== VNPay Payment Debug ===");
            System.out.println("Hash data: " + hashData.toString());
            System.out.println("Generated hash: " + vnp_SecureHash);
            System.out.println("Payment URL length: " + paymentUrl.length());
            System.out.println("Payment URL: " + paymentUrl);
            System.out.println("=========================");
            
            return "redirect:" + paymentUrl;

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Payment creation error: " + e.getMessage());
            System.err.println("Stack trace: ");
            for (StackTraceElement element : e.getStackTrace()) {
                System.err.println("  at " + element);
            }
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra khi tạo thanh toán: " + e.getMessage());
            return "redirect:/services";
        }
    }

    @GetMapping("/vnpay-return")
    public String vnpayReturn(@RequestParam Map<String, String> allParams,
                              HttpServletRequest request,
                              Model model) {

        try {
            // Enhanced debug logging
            System.out.println("=== VNPay Return Processing ===");
            System.out.println("Received parameters count: " + allParams.size());
            allParams.forEach((key, value) -> System.out.println(key + ": " + value));
            System.out.println("===============================");

            // Verify the signature first
            Map<String, String> fields = new HashMap<>(allParams);
            String vnp_SecureHash = fields.remove("vnp_SecureHash");
            fields.remove("vnp_SecureHashType");

            // Log chi tiết dữ liệu hash khi nhận callback
            List<String> fieldNames = new ArrayList<>(fields.keySet());
            Collections.sort(fieldNames);
            StringBuilder hashData = new StringBuilder();
            for (int i = 0; i < fieldNames.size(); i++) {
                String fieldName = fieldNames.get(i);
                String fieldValue = fields.get(fieldName);
                if (fieldValue != null && !fieldValue.isEmpty()) {
                    if (hashData.length() > 0) hashData.append("&");
                    hashData.append(fieldName).append("=").append(fieldValue);
                }
            }
            System.out.println("[VNPay RETURN] Hash data: " + hashData.toString());

            String signValue = VNPayConfig.hmacSHA512(VNPayConfig.secretKey, hashData.toString());
            System.out.println("[VNPay RETURN] Generated hash: " + signValue);
            System.out.println("[VNPay RETURN] Received hash: " + vnp_SecureHash);
            System.out.println("Hash match: " + signValue.equals(vnp_SecureHash));
            System.out.println("==============================");
            
            if (!signValue.equals(vnp_SecureHash)) {
                System.err.println("Invalid signature!");
                model.addAttribute("error", "Chữ ký không hợp lệ!");
                return "error";
            }

            String vnp_ResponseCode = allParams.get("vnp_ResponseCode");
            System.out.println("VNPay Response Code: " + vnp_ResponseCode);

            // Handle different response codes
            if (!"00".equals(vnp_ResponseCode)) {
                String errorMessage = getVNPayErrorMessage(vnp_ResponseCode);
                System.err.println("Payment failed with code: " + vnp_ResponseCode);
                System.err.println("Error message: " + errorMessage);
                
                model.addAttribute("error", errorMessage);
                model.addAttribute("responseCode", vnp_ResponseCode);
                model.addAttribute("transactionRef", allParams.get("vnp_TxnRef"));
                return "error";
            }

            System.out.println("Payment successful, processing transaction...");

            // Process successful payment
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String email = auth.getName();
            AppUser user = appUserRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

            System.out.println("Processing for user: " + user.getEmail());

            String orderInfo = allParams.get("vnp_OrderInfo");
            // Extract service ID from order info
            String[] parts = orderInfo.split("ID: ");
            if (parts.length < 2) {
                throw new RuntimeException("Invalid order info format: " + orderInfo);
            }
            Long serviceId = Long.parseLong(parts[1].trim());
            
            System.out.println("Service ID from order: " + serviceId);
            
            Services service = servicesRepository.findById(serviceId)
                .orElseThrow(() -> new RuntimeException("Service not found with ID: " + serviceId));

            System.out.println("Found service: " + service.getName());

            // Check for existing active transaction
            Optional<Transactions> existingTransaction = transactionsRepository
                .findActiveTransactionByUserAndService(user, serviceId, LocalDateTime.now());

            if (existingTransaction.isPresent()) {
                System.err.println("User already has active transaction for this service!");
                model.addAttribute("error", "Bạn đã có gói dịch vụ này đang hoạt động!");
                return "error";
            }

            // Update user role
            System.out.println("Updating user role to ADMINDICHVU");
            user.setRole("ADMINDICHVU");
            appUserRepository.save(user);

            // Update SecurityContextHolder
            List<org.springframework.security.core.authority.SimpleGrantedAuthority> updatedAuthorities =
                java.util.Collections.singletonList(
                    new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMINDICHVU"));
            org.springframework.security.core.userdetails.UserDetails updatedUserDetails = 
                org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())
                .authorities(updatedAuthorities)
                .build();
            org.springframework.security.authentication.UsernamePasswordAuthenticationToken newAuth =
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                    updatedUserDetails, null, updatedAuthorities);
            SecurityContextHolder.getContext().setAuthentication(newAuth);

            System.out.println("Updated user authentication context");

            // Add to ServicePackMember if not exists
            ServicePack servicePack = servicePackRepository.findById(serviceId).orElse(null);
            if (servicePack != null && !servicePackMemberRepository.existsByUserAndServicePack(user, servicePack)) {
                ServicePackMember member = new ServicePackMember(user, servicePack);
                servicePackMemberRepository.save(member);
                System.out.println("Added user to service pack: " + servicePack.getName());
            }

            // Create transaction record
            System.out.println("Creating transaction record...");
            Transactions transaction = new Transactions();
            transaction.setUser(user);
            transaction.setService(service);
            // Convert amount back from VNPay format (divide by 100)
            String vnpAmount = allParams.get("vnp_Amount");
            BigDecimal amount = new BigDecimal(vnpAmount).divide(BigDecimal.valueOf(100));
            transaction.setAmount(amount);
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
            System.out.println("Transaction saved with ID: " + transaction.getId());

            // Log the purchase
            String detail = String.format("Mua gói dịch vụ '%s' trị giá %.0f VND, thời hạn %d ngày. Mã giao dịch VNPay: %s",
                service.getName(),
                transaction.getAmount().doubleValue(),
                service.getDuration(),
                transaction.getVnpTransactionNo());

            logService.log(user.getEmail(), "Mua dịch vụ", detail);
            System.out.println("Purchase logged successfully");

            // Prepare model for receipt page
            model.addAttribute("amount", transaction.getAmount());
            model.addAttribute("bankcode", transaction.getBankCode());
            model.addAttribute("orderinfo", orderInfo);
            model.addAttribute("expireDate", transaction.getExpireDate());
            model.addAttribute("transactionNo", transaction.getVnpTransactionNo());
            model.addAttribute("payDate", transaction.getVnpPayDate());
            model.addAttribute("serviceName", service.getName());

            System.out.println("=== Payment Processing Complete ===");
            System.out.println("Transaction No: " + transaction.getVnpTransactionNo());
            System.out.println("Amount: " + transaction.getAmount());
            System.out.println("Service: " + service.getName());
            System.out.println("Expire Date: " + transaction.getExpireDate());
            System.out.println("================================");

            return "receipt";

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Payment return processing error: " + e.getMessage());
            System.err.println("Full stack trace:");
            for (StackTraceElement element : e.getStackTrace()) {
                System.err.println("  at " + element);
            }
            
            model.addAttribute("error", "Có lỗi xảy ra khi xử lý kết quả thanh toán: " + e.getMessage());
            model.addAttribute("errorDetails", e.toString());
            return "error";
        }
    }

    /**
     * Get user-friendly error message for VNPay response codes
     * @param responseCode VNPay response code
     * @return User-friendly error message in Vietnamese
     */
    private String getVNPayErrorMessage(String responseCode) {
        switch (responseCode) {
            case "07":
                return "Trừ tiền thành công. Giao dịch bị nghi ngờ (liên quan tới lừa đảo, giao dịch bất thường).";
            case "09":
                return "Giao dịch không thành công do: Thẻ/Tài khoản của khách hàng chưa đăng ký dịch vụ InternetBanking tại ngân hàng.";
            case "10":
                return "Giao dịch không thành công do: Khách hàng xác thực thông tin thẻ/tài khoản không đúng quá 3 lần.";
            case "11":
                return "Giao dịch không thành công do: Đã hết hạn chờ thanh toán. Xin quý khách vui lòng thực hiện lại giao dịch.";
            case "12":
                return "Giao dịch không thành công do: Thẻ/Tài khoản của khách hàng bị khóa.";
            case "13":
                return "Giao dịch không thành công do Quý khách nhập sai mật khẩu xác thực giao dịch (OTP). Xin quý khách vui lòng thực hiện lại giao dịch.";
            case "24":
                return "Giao dịch không thành công do: Khách hàng hủy giao dịch.";
            case "51":
                return "Giao dịch không thành công do: Tài khoản của quý khách không đủ số dư để thực hiện giao dịch.";
            case "65":
                return "Giao dịch không thành công do: Tài khoản của Quý khách đã vượt quá hạn mức giao dịch trong ngày.";
            case "75":
                return "Ngân hàng thanh toán đang bảo trì.";
            case "79":
                return "Giao dịch không thành công do: KH nhập sai mật khẩu thanh toán quá số lần quy định. Xin quý khách vui lòng thực hiện lại giao dịch.";
            case "99":
                return "Các lỗi khác (lỗi còn lại, không có trong danh sách mã lỗi đã liệt kê).";
            default:
                return "Thanh toán thất bại. Mã lỗi: " + responseCode + ". Vui lòng liên hệ hỗ trợ hoặc thử lại sau.";
        }
    }

    /**
     * Additional endpoint to check transaction status
     */
    @GetMapping("/transaction-status")
    public String checkTransactionStatus(
            @RequestParam("txnRef") String txnRef,
            Model model) {
        
        try {
            System.out.println("Checking transaction status for: " + txnRef);
            
            Optional<Transactions> transaction = transactionsRepository.findByVnpTxnRef(txnRef);
            
            if (transaction.isPresent()) {
                Transactions trans = transaction.get();
                model.addAttribute("transaction", trans);
                model.addAttribute("found", true);
                
                // Check if transaction is still active
                boolean isActive = trans.getExpireDate().isAfter(LocalDateTime.now());
                model.addAttribute("isActive", isActive);
                
                System.out.println("Transaction found: " + trans.getVnpTransactionNo());
                System.out.println("Status: " + trans.getStatus());
                System.out.println("Active: " + isActive);
            } else {
                model.addAttribute("found", false);
                model.addAttribute("txnRef", txnRef);
                System.out.println("Transaction not found for txnRef: " + txnRef);
            }
            
            return "transaction-status";
            
        } catch (Exception e) {
            System.err.println("Error checking transaction status: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Có lỗi xảy ra khi kiểm tra trạng thái giao dịch: " + e.getMessage());
            return "error";
        }
    }
}