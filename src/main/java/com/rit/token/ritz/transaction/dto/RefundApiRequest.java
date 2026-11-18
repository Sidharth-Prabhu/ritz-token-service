package com.rit.token.ritz.transaction.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class RefundApiRequest {

    @NotBlank(message = "transaction_id is required")
    @JsonProperty("transaction_id")
    private String transactionId;

    @NotNull(message = "amount is required")
    @Min(value = 1, message = "amount must be greater than zero")
    private Long amount;

    @NotBlank(message = "reason is required")
    private String reason;

    @JsonProperty("reference_id")
    private String referenceId;

    public RefundApiRequest() {}

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public Long getAmount() { return amount; }
    public void setAmount(Long amount) { this.amount = amount; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getReferenceId() { return referenceId; }
    public void setReferenceId(String referenceId) { this.referenceId = referenceId; }
}
