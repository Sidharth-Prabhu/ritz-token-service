package com.rit.token.ritz.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRecordRepository extends JpaRepository<PaymentRecord, Long> {
    Optional<PaymentRecord> findByPaymentId(String paymentId);
    Optional<PaymentRecord> findByOrderId(String orderId);
    boolean existsByPaymentId(String paymentId);
}
