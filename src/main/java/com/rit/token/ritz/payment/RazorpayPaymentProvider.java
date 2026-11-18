package com.rit.token.ritz.payment;

import com.razorpay.RazorpayClient;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Component("razorpayPaymentProvider")
public class RazorpayPaymentProvider implements PaymentGatewayService {

    private static final Logger log = LoggerFactory.getLogger(RazorpayPaymentProvider.class);

    @Value("${payment.razorpay.enabled:false}")
    private boolean enabled;

    @Value("${payment.razorpay.key-id:rzp_test_placeholder}")
    private String keyId;

    @Value("${payment.razorpay.key-secret:rzp_secret_placeholder}")
    private String keySecret;

    @Value("${payment.razorpay.webhook-secret:rzp_webhook_secret_placeholder}")
    private String webhookSecret;

    @Override
    public String getProviderName() {
        return "RAZORPAY";
    }

    @Override
    public boolean isLiveGatewayEnabled() {
        return enabled;
    }

    @Override
    public String getKeyId() {
        return keyId;
    }

    @Override
    public PaymentOrderResult createOrder(String internalOrderId, BigDecimal amountRupees, String studentId, String receipt) {
        if (!enabled) {
            log.warn("Razorpay is disabled by configuration (compliance hold). Cannot create live order.");
            return PaymentOrderResult.failure("Razorpay payment gateway is currently disabled pending merchant and compliance approval.");
        }
        try {
            RazorpayClient razorpay = new RazorpayClient(keyId, keySecret);
            JSONObject orderRequest = new JSONObject();
            // Razorpay accepts amount in paise (1 INR = 100 paise)
            long amountPaise = amountRupees.multiply(new BigDecimal("100")).longValue();
            orderRequest.put("amount", amountPaise);
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", internalOrderId);

            JSONObject notes = new JSONObject();
            notes.put("student_id", studentId);
            orderRequest.put("notes", notes);

            com.razorpay.Order order = razorpay.orders.create(orderRequest);
            String gatewayOrderId = order.get("id");
            return PaymentOrderResult.success(gatewayOrderId, amountRupees, "INR", keyId);
        } catch (Exception e) {
            log.error("Failed to create Razorpay order for {}: {}", internalOrderId, e.getMessage(), e);
            return PaymentOrderResult.failure("Payment gateway error: " + e.getMessage());
        }
    }

    @Override
    public boolean verifyPaymentSignature(String orderId, String paymentId, String signature) {
        if (!enabled) return false;
        try {
            String payload = orderId + "|" + paymentId;
            return calculateHmacSha256(payload, keySecret).equals(signature);
        } catch (Exception e) {
            log.error("Error verifying Razorpay payment signature: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean verifyWebhookSignature(String rawRequestBody, String signatureHeader) {
        if (!enabled) return false;
        if (signatureHeader == null || signatureHeader.isBlank()) return false;
        try {
            String calculated = calculateHmacSha256(rawRequestBody, webhookSecret);
            return MessageDigest.isEqual(calculated.getBytes(StandardCharsets.UTF_8), signatureHeader.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("Error verifying Razorpay webhook signature: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public PaymentDetails fetchPaymentDetails(String paymentId) {
        if (!enabled) return null;
        try {
            RazorpayClient razorpay = new RazorpayClient(keyId, keySecret);
            com.razorpay.Payment payment = razorpay.payments.fetch(paymentId);
            int amountPaise = payment.get("amount");
            BigDecimal amountRupees = new BigDecimal(amountPaise).divide(new BigDecimal("100"));
            String status = payment.get("status");
            String method = payment.get("method");
            String orderId = payment.has("order_id") ? payment.get("order_id") : null;
            return new PaymentDetails(paymentId, orderId, amountRupees, status, method);
        } catch (Exception e) {
            log.error("Failed to fetch Razorpay payment details for {}: {}", paymentId, e.getMessage());
            return null;
        }
    }

    private String calculateHmacSha256(String data, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKey);
        byte[] hmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hmac);
    }
}
