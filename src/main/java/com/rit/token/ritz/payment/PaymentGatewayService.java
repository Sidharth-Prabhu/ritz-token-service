package com.rit.token.ritz.payment;

import java.math.BigDecimal;

public interface PaymentGatewayService {
    String getProviderName();
    boolean isLiveGatewayEnabled();
    String getKeyId();
    PaymentOrderResult createOrder(String internalOrderId, BigDecimal amountRupees, String studentId, String receipt);
    boolean verifyPaymentSignature(String orderId, String paymentId, String signature);
    boolean verifyWebhookSignature(String rawRequestBody, String signatureHeader);
    PaymentDetails fetchPaymentDetails(String paymentId);
}
