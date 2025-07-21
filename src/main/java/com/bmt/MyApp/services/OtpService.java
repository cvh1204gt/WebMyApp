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

/**
 * Service for handling OTP (One-Time Password) generation, verification, and cleanup.
 */
@Service
public class OtpService {

    @Autowired
    private OtpTokenRepository otpTokenRepository;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private SystemLogRepository logRepository;

    /**
     * Generates a 6-digit OTP, saves it, sends it to the user's email, and logs the action.
     * @param email the user's email address
     * @return the generated OTP
     */
    public String generateAndSendOtp(String email) {
        String otp = String.format("%06d", new Random().nextInt(999999));
        otpTokenRepository.deleteByEmail(email);
        OtpToken otpToken = new OtpToken();
        otpToken.setEmail(email);
        otpToken.setOtp(otp);
        otpToken.setExpiryTime(LocalDateTime.now().plusMinutes(5));
        otpToken.setUsed(false);
        otpTokenRepository.save(otpToken);
        emailService.sendOtpEmail(email, otp);
        SystemLog log = SystemLog.builder()
                .username(email)
                .action("SEND_OTP")
                .detail("Đã gửi mã OTP " + otp + " đến email " + email)
                .timestamp(LocalDateTime.now())
                .build();
        logRepository.save(log);
        return otp;
    }

    /**
     * Verifies the provided OTP for the given email. Marks OTP as used and user as verified if valid.
     * @param email the user's email address
     * @param otp the OTP to verify
     * @return true if OTP is valid and verified, false otherwise
     */
    public boolean verifyOtp(String email, String otp) {
        Optional<OtpToken> otpTokenOpt = otpTokenRepository.findByEmailAndOtp(email, otp);
        if (otpTokenOpt.isPresent()) {
            OtpToken otpToken = otpTokenOpt.get();
            if (otpToken.isUsed()) {
                return false;
            }
            if (otpToken.getExpiryTime().isBefore(LocalDateTime.now())) {
                return false;
            }
            otpToken.setUsed(true);
            otpTokenRepository.save(otpToken);
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

    /**
     * Deletes all expired OTP tokens from the repository.
     */
    public void cleanExpiredOtps() {
        otpTokenRepository.deleteByExpiryTimeBefore(LocalDateTime.now());
    }
}
