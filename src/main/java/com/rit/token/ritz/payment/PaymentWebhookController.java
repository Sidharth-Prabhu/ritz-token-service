package com.rit.token.ritz.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rit.token.ritz.audit.AuditService;
import com.rit.token.ritz.transaction.WalletTransaction;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "Payment Webhooks & Callbacks", description = "Razorpay Webhook receiver and simulated Sandbox payment confirmation")
public class PaymentWebhookController {

    private static final Logger log = LoggerFactory.getLogger(PaymentWebhookController.class);

    private final PaymentGatewayFactory gatewayFactory;
    private final RechargeService rechargeService;
    private final WebhookEventRepository webhookEventRepository;
    private final RechargeOrderRepository rechargeOrderRepository;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public PaymentWebhookController(
            PaymentGatewayFactory gatewayFactory,
            RechargeService rechargeService,
            WebhookEventRepository webhookEventRepository,
            RechargeOrderRepository rechargeOrderRepository,
            AuditService auditService,
            ObjectMapper objectMapper) {
        this.gatewayFactory = gatewayFactory;
        this.rechargeService = rechargeService;
        this.webhookEventRepository = webhookEventRepository;
        this.rechargeOrderRepository = rechargeOrderRepository;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/razorpay/webhook")
    @Operation(summary = "Razorpay webhook signature verification and automated wallet credit")
    public ResponseEntity<String> handleRazorpayWebhook(
            @RequestBody String rawPayload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signatureHeader) {

        PaymentGatewayService provider = gatewayFactory.getProvider("RAZORPAY");

        // Step 9: Validate webhook signature using the raw request body
        if (!provider.verifyWebhookSignature(rawPayload, signatureHeader)) {
            log.warn("Invalid Razorpay webhook signature received");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        }

        try {
            JsonNode root = objectMapper.readTree(rawPayload);
            String eventType = root.path("event").asText();
            String eventId = root.has("event_id") ? root.get("event_id").asText() : "evt_" + hashString(rawPayload);

            // Step 11: Deduplication check
            if (webhookEventRepository.existsByEventId(eventId)) {
                log.info("Duplicate webhook event ignored: {}", eventId);
                return ResponseEntity.ok("Event already processed");
            }

            WebhookEvent webhookEvent = new WebhookEvent(eventId, "RAZORPAY", eventType, hashString(rawPayload), "RECEIVED");
            webhookEventRepository.save(webhookEvent);

            if ("order.paid".equals(eventType) || "payment.captured".equals(eventType)) {
                JsonNode paymentEntity = root.path("payload").path("payment").path("entity");
                String paymentId = paymentEntity.path("id").asText();
                String orderId = paymentEntity.path("notes").path("receipt").asText(null);
                String gatewayOrderId = paymentEntity.path("order_id").asText();
                String method = paymentEntity.path("method").asText("UPI");

                if (orderId == null || orderId.isBlank()) {
                    Optional<RechargeOrder> orderOpt = rechargeOrderRepository.findByGatewayOrderId(gatewayOrderId);
                    if (orderOpt.isPresent()) {
                        orderId = orderOpt.get().getOrderId();
                    }
                }

                if (orderId != null) {
                    rechargeService.processPaymentConfirmation(orderId, paymentId, "webhook_verified", method);
                    webhookEvent.setStatus("PROCESSED");
                    webhookEvent.setProcessedAt(LocalDateTime.now());
                    webhookEventRepository.save(webhookEvent);
                }
            }

            return ResponseEntity.ok("Webhook processed");
        } catch (Exception e) {
            log.error("Error processing Razorpay webhook: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Processing error");
        }
    }

    @GetMapping("/config")
    @Operation(summary = "Get active payment gateway client configuration (keyId and gateway provider)")
    public ResponseEntity<Map<String, Object>> getPaymentConfig() {
        return ResponseEntity.ok(Map.of(
                "provider", gatewayFactory.getProvider().getProviderName(),
                "key_id", gatewayFactory.getActiveKeyId(),
                "razorpay_enabled", gatewayFactory.isRazorpayConfigured()
        ));
    }

    @PostMapping("/verify")
    @Operation(summary = "Verify Razorpay payment signature from frontend checkout callback")
    public ResponseEntity<WalletTransaction> verifyPayment(@RequestBody Map<String, String> request) {
        String orderId = request.get("order_id");
        String paymentId = request.get("payment_id");
        String signature = request.get("signature");
        String method = request.getOrDefault("method", "RAZORPAY");

        if (orderId == null || paymentId == null) {
            throw new IllegalArgumentException("order_id and payment_id are required");
        }

        WalletTransaction txn = rechargeService.processPaymentConfirmation(orderId, paymentId, signature, method);
        return ResponseEntity.ok(txn);
    }

    @PostMapping("/sandbox/confirm")
    @Operation(summary = "Simulate payment confirmation for Sandbox/Development testing")
    public ResponseEntity<WalletTransaction> confirmSandboxPayment(@RequestBody Map<String, String> request) {
        String orderId = request.get("order_id");
        String paymentId = request.getOrDefault("payment_id", "pay_sbx_" + System.currentTimeMillis());
        String signature = request.getOrDefault("signature", "mock_sig_valid");
        String method = request.getOrDefault("method", "UPI");

        WalletTransaction txn = rechargeService.processPaymentConfirmation(orderId, paymentId, signature, method);
        return ResponseEntity.ok(txn);
    }

    private String hashString(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(input.getBytes()));
        } catch (Exception e) {
            return String.valueOf(input.hashCode());
        }
    }
}
