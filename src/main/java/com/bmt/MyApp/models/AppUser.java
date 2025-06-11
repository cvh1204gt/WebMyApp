package com.bmt.MyApp.models;

// import java.time.LocalDateTime;
// import java.util.Collection;
import java.util.Date;
// import java.util.List;

// import org.springframework.security.core.GrantedAuthority;
// import org.springframework.security.core.userdetails.UserDetails;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class AppUser {

  // THUOC TINH

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private int id;
  private String fullName;

  @Column(unique = true, nullable = false)
  private String email;
  private String phone;
  private String address;
  private String password;
  private String role;
  private Date CreateAt;
  private String verficationToken;
  private boolean isVerified;

  @Column(name = "reset_token")
  private String resetToken;

  // GETTER & SETTER

  public String getVerficationToken() {
    return verficationToken;
  }

  public void setVerficationToken(String verficationToken) {
    this.verficationToken = verficationToken;
  }

  public boolean isVerified() {
    return isVerified;
  }

  public void setVerified(boolean isVerified) {
    this.isVerified = isVerified;
  }

  public String getFullName() {
    return fullName;
  }

  public void setFullName(String fullName) {
    this.fullName = fullName;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }

  public Date getCreateAt() {
    return CreateAt;
  }

  public void setCreateAt(Date createAt) {
    CreateAt = createAt;
  }
}
