package com.rit.token.ritz.transaction.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.rit.token.ritz.transaction.TransactionStatus;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransactionApiResponse {

    private boolean success;

    @JsonProperty("transaction_id")
    private String transactionId;

    @JsonProperty("reference_id")
    private String referenceId;

    private Long amount;

    @JsonProperty("previous_balance")
    private Long previousBalance;

    @JsonProperty("current_balance")
    private Long currentBalance;

    private TransactionStatus status;

    private String message;

    public TransactionApiResponse() {}

    public static TransactionApiResponse success(String transactionId, String referenceId, Long amount, Long previousBalance, Long currentBalance, TransactionStatus status) {
        TransactionApiResponse res = new TransactionApiResponse();
        res.success = true;
        res.transactionId = transactionId;
        res.referenceId = referenceId;
        res.amount = amount;
        res.previousBalance = previousBalance;
        res.currentBalance = currentBalance;
        res.status = status;
        return res;
    }

    public static TransactionApiResponse failure(String message, String referenceId) {
        TransactionApiResponse res = new TransactionApiResponse();
        res.success = false;
        res.message = message;
        res.referenceId = referenceId;
        res.status = TransactionStatus.FAILED;
        return res;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String getReferenceId() { return referenceId; }
    public void setReferenceId(String referenceId) { this.referenceId = referenceId; }

    public Long getAmount() { return amount; }
    public void setAmount(Long amount) { this.amount = amount; }

    public Long getPreviousBalance() { return previousBalance; }
    public void setPreviousBalance(Long previousBalance) { this.previousBalance = previousBalance; }

    public Long getCurrentBalance() { return currentBalance; }
    public void setCurrentBalance(Long currentBalance) { this.currentBalance = currentBalance; }

    public TransactionStatus getStatus() { return status; }
    public void setStatus(TransactionStatus status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
