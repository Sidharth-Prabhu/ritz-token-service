package com.rit.token.ritz.transaction.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreditRequest {

    @NotBlank(message = "student_id is required")
    @JsonProperty("student_id")
    private String studentId;

    @NotNull(message = "amount is required")
    @Min(value = 1, message = "amount must be greater than zero")
    private Long amount;

    private String currency = "RITZ";

    @NotBlank(message = "reference_id is required")
    @JsonProperty("reference_id")
    private String referenceId;

    private String description;

    public CreditRequest() {}

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public Long getAmount() { return amount; }
    public void setAmount(Long amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getReferenceId() { return referenceId; }
    public void setReferenceId(String referenceId) { this.referenceId = referenceId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
