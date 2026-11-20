package com.rit.token.ritz.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public class AdminDepositResponse {

    private boolean success;

    @JsonProperty("transaction_id")
    private String transactionId;

    @JsonProperty("student_id")
    private String studentId;

    @JsonProperty("student_name")
    private String studentName;

    @JsonProperty("phone_number")
    private String phoneNumber;

    private Long amount;

    @JsonProperty("previous_balance")
    private Long previousBalance;

    @JsonProperty("new_balance")
    private Long newBalance;

    private String currency = "RITZ";

    private String category;

    @JsonProperty("reference_id")
    private String referenceId;

    @JsonProperty("voucher_id")
    private String voucherId;

    @JsonProperty("processed_by")
    private String processedBy;

    private String notes;

    private LocalDateTime timestamp = LocalDateTime.now();

    private String message;

    public AdminDepositResponse() {}

    public static AdminDepositResponse success(
            String transactionId,
            String studentId,
            String studentName,
            String phoneNumber,
            Long amount,
            Long previousBalance,
            Long newBalance,
            String category,
            String referenceId,
            String voucherId,
            String processedBy,
            String notes) {
        AdminDepositResponse res = new AdminDepositResponse();
        res.setSuccess(true);
        res.setTransactionId(transactionId);
        res.setStudentId(studentId);
        res.setStudentName(studentName);
        res.setPhoneNumber(phoneNumber);
        res.setAmount(amount);
        res.setPreviousBalance(previousBalance);
        res.setNewBalance(newBalance);
        res.setCategory(category);
        res.setReferenceId(referenceId);
        res.setVoucherId(voucherId);
        res.setProcessedBy(processedBy);
        res.setNotes(notes);
        res.setMessage("Successfully deposited " + amount + " RITZ tokens to " + studentName + " (" + studentId + ")");
        return res;
    }

    public static AdminDepositResponse failure(String message) {
        AdminDepositResponse res = new AdminDepositResponse();
        res.setSuccess(false);
        res.setMessage(message);
        return res;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
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

    public Long getPreviousBalance() {
        return previousBalance;
    }

    public void setPreviousBalance(Long previousBalance) {
        this.previousBalance = previousBalance;
    }

    public Long getNewBalance() {
        return newBalance;
    }

    public void setNewBalance(Long newBalance) {
        this.newBalance = newBalance;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getVoucherId() {
        return voucherId;
    }

    public void setVoucherId(String voucherId) {
        this.voucherId = voucherId;
    }

    public String getProcessedBy() {
        return processedBy;
    }

    public void setProcessedBy(String processedBy) {
        this.processedBy = processedBy;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
