package com.rit.token.ritz.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public class AdminUserDto {

    @JsonProperty("user_id")
    private Long userId;

    private String username;

    @JsonProperty("student_id")
    private String studentId;

    @JsonProperty("full_name")
    private String fullName;

    @JsonProperty("mobile_number")
    private String mobileNumber;

    private String email;

    private String role;

    @JsonProperty("wallet_id")
    private String walletId;

    private Long balance;

    @JsonProperty("wallet_status")
    private String walletStatus;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    public AdminUserDto() {}

    public AdminUserDto(
            Long userId,
            String username,
            String studentId,
            String fullName,
            String mobileNumber,
            String email,
            String role,
            String walletId,
            Long balance,
            String walletStatus,
            LocalDateTime createdAt) {
        this.userId = userId;
        this.username = username;
        this.studentId = studentId;
        this.fullName = fullName;
        this.mobileNumber = mobileNumber;
        this.email = email;
        this.role = role;
        this.walletId = walletId;
        this.balance = balance != null ? balance : 0L;
        this.walletStatus = walletStatus != null ? walletStatus : "ACTIVE";
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getWalletId() {
        return walletId;
    }

    public void setWalletId(String walletId) {
        this.walletId = walletId;
    }

    public Long getBalance() {
        return balance;
    }

    public void setBalance(Long balance) {
        this.balance = balance;
    }

    public String getWalletStatus() {
        return walletStatus;
    }

    public void setWalletStatus(String walletStatus) {
        this.walletStatus = walletStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
