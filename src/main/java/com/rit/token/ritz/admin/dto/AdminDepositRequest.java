package com.rit.token.ritz.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class AdminDepositRequest {

    @JsonProperty("student_id")
    private String studentId;

    @JsonProperty("phone_number")
    private String phoneNumber;

    @NotNull(message = "amount is required")
    @Min(value = 1, message = "amount must be at least 1 token")
    private Long amount;

    private String category = "MANUAL_RECHARGE"; // MERIT_GRANT, SCHOLARSHIP, CAMPUS_SUBSIDY, EVENT_PRIZE, CASH_DESK_RECHARGE, ADJUSTMENT

    private String notes;

    @JsonProperty("reference_id")
    private String referenceId;

    public AdminDepositRequest() {}

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }
}
