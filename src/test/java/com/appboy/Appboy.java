package com.appboy;

import android.content.Context;

import com.appboy.configuration.AppboyConfig;
import com.appboy.models.outgoing.AppboyProperties;
import com.mparticle.kits.AppboyPurchase;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class Appboy {
    private static Appboy instance;
    private AppboyUser currentUser;
    private List<AppboyPurchase> purchases = new ArrayList<>();

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

    }

    public void logPurchase(String sku, String currency, BigDecimal unitPrice, int quantity, AppboyProperties purchaseProperties) {
        purchases.add(new AppboyPurchase(sku, currency, unitPrice, quantity, purchaseProperties));
    }

    public List<AppboyPurchase> getPurchases() {
        return purchases;
    }

    public void clearPurchases() {
        purchases.clear();
    }
}
