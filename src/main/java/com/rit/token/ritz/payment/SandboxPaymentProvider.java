package com.rit.token.ritz.payment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.UUID;

@Component("sandboxPaymentProvider")
public class SandboxPaymentProvider implements PaymentGatewayService {

    private static final Logger log = LoggerFactory.getLogger(SandboxPaymentProvider.class);
    private static final String SANDBOX_SECRET = "sandbox_secret_key_rit_campus_2026";

    @Override
    public String getProviderName() {
        return "SANDBOX";
    }

    @Override
    public boolean isLiveGatewayEnabled() {
        return true;
    }

    @Override
    public String getKeyId() {
        return "rzp_test_sandbox_key";
    }

    @Override
    public PaymentOrderResult createOrder(String internalOrderId, BigDecimal amountRupees, String studentId, String receipt) {
        String gatewayOrderId = "order_sbx_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14);
        log.info("Sandbox order created: {} for student {} amount ₹{}", gatewayOrderId, studentId, amountRupees);
        return PaymentOrderResult.success(gatewayOrderId, amountRupees, "INR", "rzp_test_sandbox_key");
    }

    @Override
    public boolean verifyPaymentSignature(String orderId, String paymentId, String signature) {
        if (signature != null && signature.startsWith("mock_sig_")) {
            return true;
        }
        try {
            String payload = orderId + "|" + paymentId;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(SANDBOX_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String expected = HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
            return expected.equals(signature);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean verifyWebhookSignature(String rawRequestBody, String signatureHeader) {
        return signatureHeader != null && (signatureHeader.startsWith("mock_webhook_") || !signatureHeader.isBlank());
    }

    @Override
    public PaymentDetails fetchPaymentDetails(String paymentId) {
        return new PaymentDetails(paymentId, "order_sbx_mock", new BigDecimal("100.00"), "captured", "upi");
    }
}
