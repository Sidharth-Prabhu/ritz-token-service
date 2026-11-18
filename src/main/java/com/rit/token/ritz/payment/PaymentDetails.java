package com.rit.token.ritz.payment;

import java.math.BigDecimal;

public class PaymentDetails {
    private String paymentId;
    private String orderId;
    private BigDecimal amountRupees;
    private String status;
    private String method;
    private String email;
    private String contact;

    public PaymentDetails() {}

    public PaymentDetails(String paymentId, String orderId, BigDecimal amountRupees, String status, String method) {
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.amountRupees = amountRupees;
        this.status = status;
        this.method = method;
    }

    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public BigDecimal getAmountRupees() { return amountRupees; }
    public void setAmountRupees(BigDecimal amountRupees) { this.amountRupees = amountRupees; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }
}
