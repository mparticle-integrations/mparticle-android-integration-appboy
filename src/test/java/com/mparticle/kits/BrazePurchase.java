package com.mparticle.kits;

import com.braze.models.outgoing.BrazeProperties;

import java.math.BigDecimal;

public class BrazePurchase {
    private String sku;
    private String currency;
    private BigDecimal unitPrice;
    private int quantity;
    private BrazeProperties purchaseProperties;

    public BrazePurchase(String sku, String currency, BigDecimal unitPrice, int quantity, BrazeProperties purchaseProperties) {
        this.sku = sku;
        this.currency = currency;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.purchaseProperties = purchaseProperties;
    }

    public String getSku() {
        return sku;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    public BrazeProperties getPurchaseProperties() {
        return purchaseProperties;
    }
}
