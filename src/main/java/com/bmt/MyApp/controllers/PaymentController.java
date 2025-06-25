package com.bmt.MyApp.controllers;

// import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
// import java.util.Enumeration;
import java.util.HashMap;
// import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.http.HttpHeaders;
// import org.springframework.http.HttpStatus;
// import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
// import org.springframework.web.bind.annotation.ResponseBody;
// import org.springframework.web.bind.annotation.RestController;

import com.bmt.MyApp.config.VNPayConfig;
import com.bmt.MyApp.models.AppUser;
import com.bmt.MyApp.models.Services;
import com.bmt.MyApp.models.Transactions;
import com.bmt.MyApp.models.Transactions.TransactionStatus;
import com.bmt.MyApp.repositories.AppUserRepository;
import com.bmt.MyApp.repositories.ServicesRepository;
import com.bmt.MyApp.repositories.TransactionsRepository;

import jakarta.servlet.http.HttpServletRequest;
// import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
// import org.springframework.security.core.context.SecurityContextHolder;

@Controller // Thay đổi từ @RestController thành @Controller
@RequestMapping("/api/payment")
public class PaymentController {

  @Autowired
  private AppUserRepository appUserRepository;

  @Autowired
  private TransactionsRepository transactionsRepository;

  @Autowired
  private ServicesRepository servicesRepository;

  @GetMapping("/create_payment_service")
  public String createPaymentForService(
      @RequestParam("id") Integer serviceId,
      @RequestParam("amount") BigDecimal amount,
      HttpServletRequest request) {

    try {
      long amountInVND = amount.multiply(new BigDecimal(100)).longValue(); // VNPay dùng đơn vị nhỏ

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
      Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
      String createDate = formatter.format(cld.getTime());
      cld.add(Calendar.MINUTE, 15);
      String expireDate = formatter.format(cld.getTime());

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

      // ✅ Redirect tới VNPay
      return "redirect:" + paymentUrl;

    } catch (Exception e) {
      // Chuyển về trang lỗi tùy ý
      return "redirect:/error";
    }
  }

  @GetMapping("/vnpay-return")
  public String vnpayReturn(
      @RequestParam Map<String, String> allParams,
      HttpServletRequest request,
      Model model) {

    String vnp_ResponseCode = allParams.get("vnp_ResponseCode");

    if ("00".equals(vnp_ResponseCode)) {
      // ✅ Lấy user đang đăng nhập
      Authentication auth = SecurityContextHolder.getContext().getAuthentication();
      String email = auth.getName(); // vì dùng email làm username
      AppUser user = appUserRepository.findByEmail(email)
          .orElseThrow(() -> new RuntimeException("User not found"));

      // ✅ Lấy gói dịch vụ
      String orderInfo = allParams.get("vnp_OrderInfo"); // vd: "Thanh toán dịch vụ ID: 2"
      Long serviceId = Long.parseLong(orderInfo.replaceAll("\\D+", "")); // lấy số từ chuỗi
      Services service = servicesRepository.findById(serviceId)
          .orElseThrow(() -> new RuntimeException("Service not found"));

      // ✅ Tạo và lưu transaction
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

      transactionsRepository.save(transaction);

      model.addAttribute("amount", transaction.getAmount());
      model.addAttribute("bankcode", transaction.getBankCode());
      model.addAttribute("orderinfo", orderInfo);
      return "receipt";
    } else {
      model.addAttribute("error", "payment_failed");
      return "error";
    }
  }

}