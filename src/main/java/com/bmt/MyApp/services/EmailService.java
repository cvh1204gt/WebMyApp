package com.bmt.MyApp.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
  
  @Autowired
    private JavaMailSender mailSender;

    public void sendOtpEmail(String toEmail, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Xác thực tài khoản - MyApp");
        message.setText("Mã OTP của bạn là: " + otp + 
                       "\nMã này có hiệu lực trong vòng 5 phút." +
                       "\nVui lòng không chia sẻ mã này với bất kỳ ai.");
        message.setFrom("manhdung11092003@gmail.com");
        
        mailSender.send(message);
    }
}
