package com.rit.token.ritz.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class WalletResponse {
    private Long userId;
    private String userName;
    private String mobileNumber;
    private BigDecimal balance;
    private LocalDateTime updatedAt;

    public WalletResponse() {}

    public WalletResponse(Long userId, String userName, String mobileNumber, BigDecimal balance, LocalDateTime updatedAt) {
        this.userId = userId;
        this.userName = userName;
        this.mobileNumber = mobileNumber;
        this.balance = balance;
        this.updatedAt = updatedAt;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getMobileNumber() { return mobileNumber; }
    public void setMobileNumber(String mobileNumber) { this.mobileNumber = mobileNumber; }

    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
