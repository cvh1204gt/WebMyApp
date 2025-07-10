package com.bmt.MyApp.services;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bmt.MyApp.models.AppUser;
import com.bmt.MyApp.models.OtpToken;
import com.bmt.MyApp.models.SystemLog;
import com.bmt.MyApp.repositories.AppUserRepository;
import com.bmt.MyApp.repositories.OtpTokenRepository;
import com.bmt.MyApp.repositories.SystemLogRepository;

@Service
public class OtpService {

    @Autowired
    private OtpTokenRepository otpTokenRepository;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private SystemLogRepository logRepository; // <-- THÊM log repository

    public String generateAndSendOtp(String email) {
        // Tạo mã OTP 6 số
        String otp = String.format("%06d", new Random().nextInt(999999));

        // Xóa OTP cũ nếu có
        otpTokenRepository.deleteByEmail(email);

        // Lưu OTP mới
        OtpToken otpToken = new OtpToken();
        otpToken.setEmail(email);
        otpToken.setOtp(otp);
        otpToken.setExpiryTime(LocalDateTime.now().plusMinutes(5)); // Hết hạn sau 5 phút
        otpToken.setUsed(false);

        otpTokenRepository.save(otpToken);

        // Gửi email
        emailService.sendOtpEmail(email, otp);

        // Ghi log gửi OTP
        SystemLog log = SystemLog.builder()
                .username(email)
                .action("SEND_OTP")
                .detail("Đã gửi mã OTP " + otp + " đến email " + email)
                .timestamp(LocalDateTime.now())
                .build();

        logRepository.save(log); // <-- LƯU LOG

        return otp;
    }

    public boolean verifyOtp(String email, String otp) {
        Optional<OtpToken> otpTokenOpt = otpTokenRepository.findByEmailAndOtp(email, otp);

        if (otpTokenOpt.isPresent()) {
            OtpToken otpToken = otpTokenOpt.get();

            // Kiểm tra OTP đã được sử dụng chưa
            if (otpToken.isUsed()) {
                return false;
            }

            // Kiểm tra OTP còn hạn không
            if (otpToken.getExpiryTime().isBefore(LocalDateTime.now())) {
                return false;
            }

            // Đánh dấu OTP đã sử dụng
            otpToken.setUsed(true);
            otpTokenRepository.save(otpToken);

            // Cập nhật trạng thái verified cho user
            Optional<AppUser> userOpt = userRepository.findByEmail(email);
            if (userOpt.isPresent()) {
                AppUser user = userOpt.get();
                user.setVerified(true);
                userRepository.save(user);
            }

            return true;
        }

        return false;
    }

    public void cleanExpiredOtps() {
        otpTokenRepository.deleteByExpiryTimeBefore(LocalDateTime.now());
    }
}
