package com.mparticle.kits.mocks;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

public class MockContextApplication extends Application {
    @Override
    public Context getApplicationContext() {
        return this;
    }

    @Override
    public SharedPreferences getSharedPreferences(String name, int mode) {
        return new MockSharedPreferences();
    }

    @Override
    public void registerActivityLifecycleCallbacks(ActivityLifecycleCallbacks callback) {
        //do nothing
    }
}
