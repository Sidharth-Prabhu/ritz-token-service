package com.rit.token.ritz.dto;

import java.math.BigDecimal;
import java.util.Map;

public class StatsResponse {
    private BigDecimal totalCirculation;
    private long activeWallets;
    private long totalUsers;
    private long serializedUnitsTotal;
    private long activeSerializedUnits;

    public StatsResponse() {}

    public StatsResponse(BigDecimal totalCirculation, long activeWallets, long totalUsers, long serializedUnitsTotal, long activeSerializedUnits) {
        this.totalCirculation = totalCirculation;
        this.activeWallets = activeWallets;
        this.totalUsers = totalUsers;
        this.serializedUnitsTotal = serializedUnitsTotal;
        this.activeSerializedUnits = activeSerializedUnits;
    }

    public BigDecimal getTotalCirculation() { return totalCirculation; }
    public void setTotalCirculation(BigDecimal totalCirculation) { this.totalCirculation = totalCirculation; }

    public long getActiveWallets() { return activeWallets; }
    public void setActiveWallets(long activeWallets) { this.activeWallets = activeWallets; }

    public long getTotalUsers() { return totalUsers; }
    public void setTotalUsers(long totalUsers) { this.totalUsers = totalUsers; }

    public long getSerializedUnitsTotal() { return serializedUnitsTotal; }
    public void setSerializedUnitsTotal(long serializedUnitsTotal) { this.serializedUnitsTotal = serializedUnitsTotal; }

    public long getActiveSerializedUnits() { return activeSerializedUnits; }
    public void setActiveSerializedUnits(long activeSerializedUnits) { this.activeSerializedUnits = activeSerializedUnits; }

    public Map<String, Object> toMap() {
        return Map.of(
            "totalCirculation", totalCirculation != null ? totalCirculation : BigDecimal.ZERO,
            "activeWallets", activeWallets,
            "totalUsers", totalUsers,
            "serializedUnitsTotal", serializedUnitsTotal,
            "activeSerializedUnits", activeSerializedUnits
        );
    }
}
