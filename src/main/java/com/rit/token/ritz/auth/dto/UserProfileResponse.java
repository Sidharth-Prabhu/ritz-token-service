package com.rit.token.ritz.auth.dto;

import com.rit.token.ritz.auth.Role;

public class UserProfileResponse {
    private Long id;
    private String username;
    private String fullName;
    private String email;
    private Role role;
    private String studentId;
    private String merchantId;
    private String applicationId;
    private boolean active;

    public UserProfileResponse() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

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

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
