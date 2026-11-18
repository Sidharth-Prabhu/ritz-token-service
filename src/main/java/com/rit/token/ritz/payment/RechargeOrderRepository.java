package com.rit.token.ritz.payment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RechargeOrderRepository extends JpaRepository<RechargeOrder, Long> {
    Optional<RechargeOrder> findByOrderId(String orderId);
    Optional<RechargeOrder> findByGatewayOrderId(String gatewayOrderId);
    List<RechargeOrder> findByStudentIdOrderByCreatedAtDesc(String studentId);
    Page<RechargeOrder> findByStudentIdOrderByCreatedAtDesc(String studentId, Pageable pageable);
    Page<RechargeOrder> findAllByOrderByCreatedAtDesc(Pageable pageable);
    long countByStatus(PaymentOrderStatus status);
}
