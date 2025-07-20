package com.bmt.MyApp.config;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;

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

    // FIX: Thêm method để lấy timezone Việt Nam chính xác
    public static TimeZone getVietnamTimeZone() {
        return TimeZone.getTimeZone("Asia/Ho_Chi_Minh");
    }

    // FIX: Method tạo thời gian chuẩn cho VNPay
    public static String formatVNPayDateTime(Calendar calendar) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        formatter.setTimeZone(getVietnamTimeZone());
        return formatter.format(calendar.getTime());
    }

    // FIX: Method tạo thời gian hiện tại theo timezone Việt Nam
    public static String getCurrentVNPayDateTime() {
        Calendar calendar = Calendar.getInstance(getVietnamTimeZone());
        return formatVNPayDateTime(calendar);
    }

    // FIX: Method tạo thời gian hết hạn với timeout tùy chỉnh
    public static String getExpireDateTime(int timeoutMinutes) {
        Calendar calendar = Calendar.getInstance(getVietnamTimeZone());
        calendar.add(Calendar.MINUTE, timeoutMinutes);
        return formatVNPayDateTime(calendar);
    }

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

    // Improved hashAllFields method với better logging
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
        
        String hashData = sb.toString();
        String hash = hmacSHA512(secretKey, hashData);
        
        // Debug logging
        System.out.println("Hash calculation:");
        System.out.println("Hash data: " + hashData);
        System.out.println("Secret key length: " + secretKey.length());
        System.out.println("Generated hash: " + hash);
        
        return hash;
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
            System.err.println("Error generating HMAC-SHA512: " + ex.getMessage());
            ex.printStackTrace();
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
                ipAddress = request.getHeader("X-Forwarded-For");
            }
            if (ipAddress == null || ipAddress.isEmpty()) {
                ipAddress = request.getRemoteAddr();
            }
            // Handle multiple IPs in X-FORWARDED-FOR
            if (ipAddress != null && ipAddress.contains(",")) {
                ipAddress = ipAddress.split(",")[0].trim();
            }
            
            // Debug log
            System.out.println("Client IP Address: " + ipAddress);
            
        } catch (Exception e) {
            System.err.println("Error getting IP address: " + e.getMessage());
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

    // FIX: Method validate response từ VNPay
    public static boolean validateVNPayResponse(Map<String, String> params) {
        try {
            Map<String, String> fields = new HashMap<>(params);
            String receivedHash = fields.remove("vnp_SecureHash");
            fields.remove("vnp_SecureHashType");
            
            if (receivedHash == null || receivedHash.isEmpty()) {
                System.err.println("Missing vnp_SecureHash in response");
                return false;
            }
            
            String calculatedHash = hashAllFields(fields);
            boolean isValid = calculatedHash.equals(receivedHash);
            
            if (!isValid) {
                System.err.println("Hash validation failed:");
                System.err.println("Calculated: " + calculatedHash);
                System.err.println("Received: " + receivedHash);
            }
            
            return isValid;
        } catch (Exception e) {
            System.err.println("Error validating VNPay response: " + e.getMessage());
            return false;
        }
    }
}