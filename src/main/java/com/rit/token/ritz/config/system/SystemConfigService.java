package com.rit.token.ritz.config.system;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class SystemConfigService {

    private final SystemConfigRepository repository;

    @Value("${ritz.token.exchange-rate:1.0}")
    private double defaultExchangeRate;

    @Value("${ritz.token.min-recharge-inr:10}")
    private long defaultMinRecharge;

    @Value("${ritz.token.max-recharge-inr:10000}")
    private long defaultMaxRecharge;

    @Value("${ritz.token.low-balance-threshold:50}")
    private long defaultLowBalanceThreshold;

    public SystemConfigService(SystemConfigRepository repository) {
        this.repository = repository;
    }

    public String getConfig(String key, String defaultValue) {
        return repository.findByConfigKey(key)
                .map(SystemConfig::getConfigValue)
                .orElse(defaultValue);
    }

    public BigDecimal getTokenExchangeRate() {
        String val = getConfig("TOKEN_EXCHANGE_RATE", String.valueOf(defaultExchangeRate));
        try {
            return new BigDecimal(val);
        } catch (Exception e) {
            return BigDecimal.ONE;
        }
    }

    public long getMinRechargeInr() {
        return Long.parseLong(getConfig("MIN_RECHARGE_INR", String.valueOf(defaultMinRecharge)));
    }

    public long getMaxRechargeInr() {
        return Long.parseLong(getConfig("MAX_RECHARGE_INR", String.valueOf(defaultMaxRecharge)));
    }

    public long getLowBalanceThreshold() {
        return Long.parseLong(getConfig("LOW_BALANCE_THRESHOLD", String.valueOf(defaultLowBalanceThreshold)));
    }

    public List<SystemConfig> getAllConfigs() {
        return repository.findAll();
    }

    public SystemConfig setConfig(String key, String value, String description) {
        SystemConfig config = repository.findByConfigKey(key)
                .orElse(new SystemConfig(key, value, description));
        config.setConfigValue(value);
        if (description != null) config.setDescription(description);
        return repository.save(config);
    }
}
