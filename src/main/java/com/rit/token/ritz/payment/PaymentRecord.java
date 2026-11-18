package com.rit.token.ritz.payment;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_records")
public class PaymentRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_id", nullable = false, unique = true, length = 128)
    private String paymentId;

    @Column(name = "order_id", nullable = false, length = 64)
    private String orderId;

    @Column(name = "amount_minor", nullable = false)
    private Long amountMinor;

    @Column(nullable = false, length = 16)
    private String currency = "INR";

    @Column(nullable = false, length = 32)
    private String gateway = "RAZORPAY";

    @Column(name = "gateway_status", nullable = false, length = 32)
    private String gatewayStatus;

    @Column(name = "payment_method", length = 32)
    private String paymentMethod;

    @Column(name = "error_code", length = 64)
    private String errorCode;

    @Column(name = "error_description", columnDefinition = "TEXT")
    private String errorDescription;

    @Column(name = "raw_response", columnDefinition = "TEXT")
    private String rawResponse;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public PaymentRecord() {}

    public PaymentRecord(String paymentId, String orderId, Long amountMinor, String gateway, String gatewayStatus, String paymentMethod) {
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.amountMinor = amountMinor;
        this.gateway = gateway;
        this.gatewayStatus = gatewayStatus;
        this.paymentMethod = paymentMethod;
        this.currency = "INR";
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public String getPaymentId() { return paymentId; }
    public String getOrderId() { return orderId; }
    public Long getAmountMinor() { return amountMinor; }
    public String getCurrency() { return currency; }
    public String getGateway() { return gateway; }
    public String getGatewayStatus() { return gatewayStatus; }
    public String getPaymentMethod() { return paymentMethod; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public String getErrorDescription() { return errorDescription; }
    public void setErrorDescription(String errorDescription) { this.errorDescription = errorDescription; }
    public String getRawResponse() { return rawResponse; }
    public void setRawResponse(String rawResponse) { this.rawResponse = rawResponse; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
