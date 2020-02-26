package com.mparticle.kits;

import com.appboy.models.outgoing.AppboyProperties;

import java.math.BigDecimal;

public class AppboyPurchase {
    private String sku;
    private String currency;
    private BigDecimal unitPrice;
    private int quantity;
    private AppboyProperties purchaseProperties;

    public AppboyPurchase(String sku, String currency, BigDecimal unitPrice, int quantity, AppboyProperties purchaseProperties) {
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

    public AppboyProperties getPurchaseProperties() {
        return purchaseProperties;
    }
}
