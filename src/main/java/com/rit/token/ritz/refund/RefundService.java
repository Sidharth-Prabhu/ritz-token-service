package com.rit.token.ritz.refund;

import com.rit.token.ritz.audit.AuditService;
import com.rit.token.ritz.transaction.TransactionService;
import com.rit.token.ritz.transaction.WalletTransaction;
import com.rit.token.ritz.transaction.WalletTransactionRepository;
import com.rit.token.ritz.transaction.dto.RefundApiRequest;
import com.rit.token.ritz.transaction.dto.TransactionApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefundService {

    private static final Logger log = LoggerFactory.getLogger(RefundService.class);

    private final RefundRepository refundRepository;
    private final WalletTransactionRepository transactionRepository;
    private final TransactionService transactionService;
    private final AuditService auditService;

    public RefundService(
            RefundRepository refundRepository,
            WalletTransactionRepository transactionRepository,
            TransactionService transactionService,
            AuditService auditService) {
        this.refundRepository = refundRepository;
        this.transactionRepository = transactionRepository;
        this.transactionService = transactionService;
        this.auditService = auditService;
    }

    @Transactional
    public RefundRequest createRefundRequest(String studentId, String transactionId, Long amount, String reason) {
        WalletTransaction originalTxn = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + transactionId));

        if (!studentId.equals(originalTxn.getStudentId())) {
            throw new IllegalArgumentException("Transaction does not belong to student: " + studentId);
        }

        if (amount == null || amount <= 0 || amount > originalTxn.getAmountMinorUnits()) {
            throw new IllegalArgumentException("Invalid refund amount. Max: " + originalTxn.getAmountMinorUnits());
        }

        String refundId = "REF-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase();
        RefundRequest refund = new RefundRequest(refundId, transactionId, studentId, originalTxn.getMerchantId(), amount, reason);
        RefundRequest saved = refundRepository.save(refund);

        auditService.logAction(studentId, "STUDENT", "REQUEST_REFUND", "REFUND", refundId, null, "Requested refund for " + transactionId);
        return saved;
    }

    @Transactional
    public RefundRequest reviewRefund(String refundId, boolean approve, String reviewerId) {
        RefundRequest refund = refundRepository.findByRefundId(refundId)
                .orElseThrow(() -> new IllegalArgumentException("Refund request not found: " + refundId));

        if (refund.getStatus() != RefundStatus.REQUESTED) {
            throw new IllegalStateException("Refund request is already " + refund.getStatus());
        }

        refund.setApprovedBy(reviewerId);

        if (!approve) {
            refund.setStatus(RefundStatus.REJECTED);
            auditService.logAction(reviewerId, "ADMIN", "REJECT_REFUND", "REFUND", refundId, null, "Refund rejected");
            return refundRepository.save(refund);
        }

        // Execute compensating transaction
        RefundApiRequest apiRequest = new RefundApiRequest();
        apiRequest.setTransactionId(refund.getOriginalTransactionId());
        apiRequest.setAmount(refund.getAmountMinorUnits());
        apiRequest.setReason(refund.getReason());
        apiRequest.setReferenceId(refund.getRefundId());

        TransactionApiResponse result = transactionService.executeRefund(apiRequest, refund.getRefundId(), "REFUND_SERVICE");
        if (result.isSuccess()) {
            refund.setStatus(RefundStatus.COMPLETED);
        } else {
            refund.setStatus(RefundStatus.REJECTED);
        }

        auditService.logAction(reviewerId, "ADMIN", "APPROVE_REFUND", "REFUND", refundId, null, "Refund processed");
        return refundRepository.save(refund);
    }

    public List<RefundRequest> getRefundsByStudent(String studentId) {
        return refundRepository.findByStudentIdOrderByCreatedAtDesc(studentId);
    }

    public List<RefundRequest> getRefundsByMerchant(String merchantId) {
        return refundRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId);
    }

    public Page<RefundRequest> getAllRefunds(Pageable pageable) {
        return refundRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    public Optional<RefundRequest> getRefund(String refundId) {
        return refundRepository.findByRefundId(refundId);
    }
}
