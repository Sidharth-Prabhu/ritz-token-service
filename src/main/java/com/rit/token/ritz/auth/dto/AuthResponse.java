package com.rit.token.ritz.auth.dto;

import com.rit.token.ritz.auth.Role;

public class AuthResponse {
    private boolean success;
    private String token;
    private String refreshToken;
    private String tokenType = "Bearer";
    private String username;
    private String fullName;
    private String email;
    private Role role;
    private String studentId;
    private String merchantId;
    private String applicationId;
    private String message;

    public AuthResponse() {}

    public static AuthResponse success(String token, String refreshToken, String username, String fullName, String email, Role role, String studentId, String merchantId, String applicationId) {
        AuthResponse res = new AuthResponse();
        res.success = true;
        res.token = token;
        res.refreshToken = refreshToken;
        res.username = username;
        res.fullName = fullName;
        res.email = email;
        res.role = role;
        res.studentId = studentId;
        res.merchantId = merchantId;
        res.applicationId = applicationId;
        res.message = "Authentication successful";
        return res;
    }

    public static AuthResponse failure(String message) {
        AuthResponse res = new AuthResponse();
        res.success = false;
        res.message = message;
        return res;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getMerchantId() { return merchantId; }
    public void setMerchantId(String merchantId) { this.merchantId = merchantId; }

    public String getApplicationId() { return applicationId; }
    public void setApplicationId(String applicationId) { this.applicationId = applicationId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
