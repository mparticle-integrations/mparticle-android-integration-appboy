package com.appboy;

import android.content.Context;

import com.appboy.configuration.AppboyConfig;
import com.appboy.models.outgoing.AppboyProperties;

public class Appboy {
    private static Appboy instance;
    private AppboyUser currentUser;

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
}
