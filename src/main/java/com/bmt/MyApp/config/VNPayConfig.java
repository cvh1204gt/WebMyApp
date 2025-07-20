package com.bmt.MyApp.config;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import jakarta.servlet.http.HttpServletRequest;

public class VNPayConfig {
  public static String vnp_Version = "2.1.0";
  public static String vnp_Command = "pay";
  public static String orderType = "other";

  public static String vnp_PayUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
  public static String vnp_ReturnUrl = "https://webmyapp-632e.onrender.com/api/payment/vnpay-return";
  public static String vnp_TmnCode = "C0FCDTWU";
  public static String secretKey = "YUUOURKBY3PES06XV6UUKMS8LJ0P7KGV";
  public static String vnp_ApiUrl = "https://sandbox.vnpayment.vn/merchant_webapi/api/transaction";

  public static String md5(String message) {
    String digest = null;
    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      byte[] hash = md.digest(message.getBytes("UTF-8"));
      StringBuilder sb = new StringBuilder(2 * hash.length);
      for (byte b : hash) {
        sb.append(String.format("%02x", b & 0xff));
      }
      digest = sb.toString();
    } catch (UnsupportedEncodingException ex) {
      digest = "";
    } catch (NoSuchAlgorithmException ex) {
      digest = "";
    }
    return digest;
  }

  public static String Sha256(String message) {
    String digest = null;
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] hash = md.digest(message.getBytes("UTF-8"));
      StringBuilder sb = new StringBuilder(2 * hash.length);
      for (byte b : hash) {
        sb.append(String.format("%02x", b & 0xff));
      }
      digest = sb.toString();
    } catch (UnsupportedEncodingException ex) {
      digest = "";
    } catch (NoSuchAlgorithmException ex) {
      digest = "";
    }
    return digest;
  }

  // Fixed hashAllFields method - proper parameter ordering and URL encoding
  public static String hashAllFields(Map<String, String> fields) {
    List<String> fieldNames = new ArrayList<>(fields.keySet());
    Collections.sort(fieldNames);
    StringBuilder sb = new StringBuilder();
    
    boolean first = true;
    for (String fieldName : fieldNames) {
      String fieldValue = fields.get(fieldName);
      if ((fieldValue != null) && (fieldValue.length() > 0)) {
        if (!first) {
          sb.append("&");
        }
        // Don't URL encode for hash data - VNPay expects raw values
        sb.append(fieldName).append("=").append(fieldValue);
        first = false;
      }
    }
    return hmacSHA512(secretKey, sb.toString());
  }

  public static String hmacSHA512(final String key, final String data) {
    try {
      if (key == null || data == null) {
        throw new NullPointerException("Key and data must not be null");
      }
      final Mac hmac512 = Mac.getInstance("HmacSHA512");
      byte[] hmacKeyBytes = key.getBytes(StandardCharsets.UTF_8);
      final SecretKeySpec secretKeySpec = new SecretKeySpec(hmacKeyBytes, "HmacSHA512");
      hmac512.init(secretKeySpec);
      byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
      byte[] result = hmac512.doFinal(dataBytes);
      StringBuilder sb = new StringBuilder(2 * result.length);
      for (byte b : result) {
        sb.append(String.format("%02x", b & 0xff));
      }
      return sb.toString();
    } catch (Exception ex) {
      throw new RuntimeException("Error generating HMAC-SHA512", ex);
    }
  }

  public static String getIpAddress(HttpServletRequest request) {
    String ipAddress;
    try {
      ipAddress = request.getHeader("X-FORWARDED-FOR");
      if (ipAddress == null || ipAddress.isEmpty()) {
        ipAddress = request.getHeader("X-Real-IP");
      }
      if (ipAddress == null || ipAddress.isEmpty()) {
        ipAddress = request.getRemoteAddr();
      }
      // Handle multiple IPs in X-FORWARDED-FOR
      if (ipAddress != null && ipAddress.contains(",")) {
        ipAddress = ipAddress.split(",")[0].trim();
      }
    } catch (Exception e) {
      ipAddress = "127.0.0.1"; // Default fallback
    }
    return ipAddress;
  }

  public static String getRandomNumber(int len) {
    Random rnd = new Random();
    String chars = "0123456789";
    StringBuilder sb = new StringBuilder(len);
    for (int i = 0; i < len; i++) {
      sb.append(chars.charAt(rnd.nextInt(chars.length())));
    }
    return sb.toString();
  }
}