package com.rit.token.ritz.payment;

import java.math.BigDecimal;

public class PaymentOrderResult {
    private boolean success;
    private String gatewayOrderId;
    private BigDecimal amountRupees;
    private String currency;
    private String keyId;
    private String errorMessage;

    public PaymentOrderResult() {}

    public static PaymentOrderResult success(String gatewayOrderId, BigDecimal amountRupees, String currency, String keyId) {
        PaymentOrderResult r = new PaymentOrderResult();
        r.success = true;
        r.gatewayOrderId = gatewayOrderId;
        r.amountRupees = amountRupees;
        r.currency = currency;
        r.keyId = keyId;
        return r;
    }

    public static PaymentOrderResult failure(String errorMessage) {
        PaymentOrderResult r = new PaymentOrderResult();
        r.success = false;
        r.errorMessage = errorMessage;
        return r;
    }

    public boolean isSuccess() { return success; }
    public String getGatewayOrderId() { return gatewayOrderId; }
    public BigDecimal getAmountRupees() { return amountRupees; }
    public String getCurrency() { return currency; }
    public String getKeyId() { return keyId; }
    public String getErrorMessage() { return errorMessage; }
}
