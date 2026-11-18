package com.rit.token.ritz.transaction.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class DebitRequest {

    @JsonProperty("student_id")
    private String studentId;

    @JsonProperty("phone_number")
    private String phoneNumber;

    @NotNull(message = "amount is required")
    @Min(value = 1, message = "amount must be greater than zero")
    private Long amount;

    private String currency = "RITZ";

    @NotBlank(message = "reference_id is required")
    @JsonProperty("reference_id")
    private String referenceId;

    @JsonProperty("merchant_id")
    private String merchantId;

    private String description;

    public DebitRequest() {}

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public Long getAmount() { return amount; }
    public void setAmount(Long amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getReferenceId() { return referenceId; }
    public void setReferenceId(String referenceId) { this.referenceId = referenceId; }

    public String getMerchantId() { return merchantId; }
    public void setMerchantId(String merchantId) { this.merchantId = merchantId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
