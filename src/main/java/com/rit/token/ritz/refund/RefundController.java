package com.rit.token.ritz.refund;

import com.rit.token.ritz.auth.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/refunds")
@Tag(name = "Refunds Management", description = "Student refund requests and administrative approvals")
public class RefundController {

    private final RefundService refundService;

    public RefundController(RefundService refundService) {
        this.refundService = refundService;
    }

    private String getStudentId(Authentication auth) {
        if (auth != null && auth.getPrincipal() instanceof User user && user.getStudentId() != null) {
            return user.getStudentId();
        }
        return auth != null ? auth.getName() : null;
    }

    @PostMapping("/request")
    @Operation(summary = "Initiate a student refund request")
    public ResponseEntity<RefundRequest> requestRefund(
            Authentication authentication,
            @RequestBody Map<String, Object> request) {
        String studentId = getStudentId(authentication);
        String txnId = (String) request.get("transaction_id");
        Long amount = Long.parseLong(request.get("amount").toString());
        String reason = (String) request.get("reason");

        RefundRequest refund = refundService.createRefundRequest(studentId, txnId, amount, reason);
        return ResponseEntity.ok(refund);
    }

    @GetMapping
    @Operation(summary = "Get refund requests")
    public ResponseEntity<?> getRefunds(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        boolean isAdmin = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().contains("ADMIN"));
        if (isAdmin) {
            Page<RefundRequest> all = refundService.getAllRefunds(PageRequest.of(page, size));
            return ResponseEntity.ok(all);
        } else {
            String studentId = getStudentId(authentication);
            List<RefundRequest> list = refundService.getRefundsByStudent(studentId);
            return ResponseEntity.ok(list);
        }
    }

    @PostMapping("/{refundId}/review")
    @Operation(summary = "Approve or reject a refund request (Admin/Finance only)")
    public ResponseEntity<RefundRequest> reviewRefund(
            @PathVariable String refundId,
            @RequestBody Map<String, Object> body,
            Authentication authentication) {
        boolean approve = Boolean.parseBoolean(body.get("approve").toString());
        String reviewer = authentication != null ? authentication.getName() : "ADMIN";
        RefundRequest reviewed = refundService.reviewRefund(refundId, approve, reviewer);
        return ResponseEntity.ok(reviewed);
    }
}
