package com.appboy;

import android.content.Context;

import com.appboy.configuration.AppboyConfig;
import com.appboy.models.outgoing.AppboyProperties;
import com.mparticle.kits.AppboyPurchase;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Appboy {
    private static Appboy instance;
    private AppboyUser currentUser;
    private List<AppboyPurchase> purchases = new ArrayList<>();
    private Map<String, AppboyProperties> events = new HashMap<String, AppboyProperties>();

    public static boolean configure(Context context, AppboyConfig config) {
        return true;
    }

    public static Appboy getInstance(Context context) {
        if (instance == null) {
            instance = new Appboy();
        }
        return instance;
    }

    public AppboyUser getCurrentUser() {
        if (currentUser == null) {
            currentUser = new MockAppboyUser();
        }
        return currentUser;
    }

    public void logCustomEvent(String key, AppboyProperties appboyProperties) {
        events.put(key, appboyProperties);
    }

    public void logPurchase(String sku, String currency, BigDecimal unitPrice, int quantity, AppboyProperties purchaseProperties) {
        purchases.add(new AppboyPurchase(sku, currency, unitPrice, quantity, purchaseProperties));
    }

    public List<AppboyPurchase> getPurchases() {
        return purchases;
    }

    public Map<String, AppboyProperties> getEvents() {
        return events;
    }

    public void clearPurchases() {
        purchases.clear();
    }

    public void clearEvents() {
        events.clear();
    }
}
