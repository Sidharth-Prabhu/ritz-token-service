package com.rit.token.ritz.refund;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RefundRepository extends JpaRepository<RefundRequest, Long> {
    Optional<RefundRequest> findByRefundId(String refundId);
    List<RefundRequest> findByStudentIdOrderByCreatedAtDesc(String studentId);
    Page<RefundRequest> findByStudentIdOrderByCreatedAtDesc(String studentId, Pageable pageable);
    List<RefundRequest> findByMerchantIdOrderByCreatedAtDesc(String merchantId);
    Page<RefundRequest> findByMerchantIdOrderByCreatedAtDesc(String merchantId, Pageable pageable);
    Page<RefundRequest> findAllByOrderByCreatedAtDesc(Pageable pageable);
    long countByStatus(RefundStatus status);
}
