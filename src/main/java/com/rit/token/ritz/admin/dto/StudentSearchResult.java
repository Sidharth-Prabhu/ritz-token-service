package com.rit.token.ritz.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class StudentSearchResult {

    @JsonProperty("student_id")
    private String studentId;

    @JsonProperty("student_name")
    private String studentName;

    @JsonProperty("mobile_number")
    private String mobileNumber;

    private String email;

    private String department;

    private Long balance;

    @JsonProperty("wallet_status")
    private String walletStatus;

    public StudentSearchResult() {}

    public StudentSearchResult(String studentId, String studentName, String mobileNumber, String email, String department, Long balance, String walletStatus) {
        this.studentId = studentId;
        this.studentName = studentName;
        this.mobileNumber = mobileNumber;
        this.email = email;
        this.department = department;
        this.balance = balance != null ? balance : 0L;
        this.walletStatus = walletStatus != null ? walletStatus : "ACTIVE";
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
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

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
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
}
