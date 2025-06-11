package com.bmt.MyApp.models;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

public class OtpVerificationDto {

  @NotEmpty(message = "Email không được để trống")
  @Email(message = "Email không hợp lệ")
  private String email;

  @NotEmpty(message = "Mã OTP không được để trống")
  @Pattern(regexp = "^[0-9]{6}$", message = "Mã OTP phải là 6 chữ số")
  private String otp;

  // Constructors
  public OtpVerificationDto() {
  }

  public OtpVerificationDto(String email, String otp) {
    this.email = email;
    this.otp = otp;
  }

  // Getters and Setters
  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getOtp() {
    return otp;
  }

  public void setOtp(String otp) {
    this.otp = otp;
  }
}
