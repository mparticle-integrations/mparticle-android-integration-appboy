package com.braze;

import android.content.Context;

import com.braze.configuration.BrazeConfig;
import com.braze.models.outgoing.BrazeProperties;
import com.mparticle.kits.BrazePurchase;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Braze {
    private static Braze instance;
    private MockBrazeUser currentUser;
    private List<BrazePurchase> purchases = new ArrayList<>();
    private Map<String, BrazeProperties> events = new HashMap<String, BrazeProperties>();

    public static boolean configure(Context context, BrazeConfig config) {
        return true;
    }

    public static Braze getInstance(Context context) {
        if (instance == null) {
            instance = new Braze();
        }
        return instance;
    }

    public <T extends BrazeUser> T getCurrentUser() {
        if (currentUser == null) {
            currentUser = new MockBrazeUser();
        }
        return (T)currentUser;
    }

    public void logCustomEvent(String key, BrazeProperties brazeProperties) {
        events.put(key, brazeProperties);
    }

    public void logPurchase(String sku, String currency, BigDecimal unitPrice, int quantity, BrazeProperties purchaseProperties) {
        purchases.add(new BrazePurchase(sku, currency, unitPrice, quantity, purchaseProperties));
    }

    public List<BrazePurchase> getPurchases() {
        return purchases;
    }

    public Map<String, BrazeProperties> getEvents() {
        return events;
    }

    public void clearPurchases() {
        purchases.clear();
    }

    public void clearEvents() {
        events.clear();
    }
}
