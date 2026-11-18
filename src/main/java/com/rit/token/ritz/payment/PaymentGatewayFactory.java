package com.rit.token.ritz.payment;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PaymentGatewayFactory {

    private final PaymentGatewayService razorpayProvider;
    private final PaymentGatewayService sandboxProvider;

    @Value("${payment.razorpay.enabled:false}")
    private boolean razorpayEnabled;

    public PaymentGatewayFactory(
            @Qualifier("razorpayPaymentProvider") PaymentGatewayService razorpayProvider,
            @Qualifier("sandboxPaymentProvider") PaymentGatewayService sandboxProvider) {
        this.razorpayProvider = razorpayProvider;
        this.sandboxProvider = sandboxProvider;
    }

    public PaymentGatewayService getProvider() {
        if (razorpayEnabled) {
            return razorpayProvider;
        }
        return sandboxProvider;
    }

    public PaymentGatewayService getProvider(String name) {
        if ("RAZORPAY".equalsIgnoreCase(name)) {
            return razorpayProvider;
        }
        return sandboxProvider;
    }

    public boolean isRazorpayConfigured() {
        return razorpayEnabled;
    }

    public String getActiveKeyId() {
        return getProvider().getKeyId();
    }
}
