package com.rit.token.ritz.payment;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "recharge_orders")
public class RechargeOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false, unique = true, length = 64)
    private String orderId;

    @Column(name = "student_id", nullable = false, length = 64)
    private String studentId;

    @Column(name = "amount_rupees", nullable = false, precision = 12, scale = 2)
    private BigDecimal amountRupees;

    @Column(name = "token_amount", nullable = false)
    private Long tokenAmount;

    @Column(name = "gateway_order_id", length = 128)
    private String gatewayOrderId;

    @Column(name = "payment_gateway", nullable = false, length = 32)
    private String paymentGateway = "RAZORPAY";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PaymentOrderStatus status = PaymentOrderStatus.CREATED;

    @Column(name = "payment_id", length = 128)
    private String paymentId;

    @Column(length = 255)
    private String signature;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public RechargeOrder() {}

    public RechargeOrder(String orderId, String studentId, BigDecimal amountRupees, Long tokenAmount, String paymentGateway) {
        this.orderId = orderId;
        this.studentId = studentId;
        this.amountRupees = amountRupees;
        this.tokenAmount = tokenAmount;
        this.paymentGateway = paymentGateway;
        this.status = PaymentOrderStatus.CREATED;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public BigDecimal getAmountRupees() { return amountRupees; }
    public void setAmountRupees(BigDecimal amountRupees) { this.amountRupees = amountRupees; }

    public Long getTokenAmount() { return tokenAmount; }
    public void setTokenAmount(Long tokenAmount) { this.tokenAmount = tokenAmount; }

    public String getGatewayOrderId() { return gatewayOrderId; }
    public void setGatewayOrderId(String gatewayOrderId) { this.gatewayOrderId = gatewayOrderId; }

    public String getPaymentGateway() { return paymentGateway; }
    public void setPaymentGateway(String paymentGateway) { this.paymentGateway = paymentGateway; }

    public PaymentOrderStatus getStatus() { return status; }
    public void setStatus(PaymentOrderStatus status) { this.status = status; }

    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }

    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
