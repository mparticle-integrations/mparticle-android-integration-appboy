package com.mparticle.kits.mocks

import android.app.Application
import android.content.SharedPreferences
import com.mparticle.kits.mocks.MockSharedPreferences
import android.app.Application.ActivityLifecycleCallbacks
import android.content.Context

class MockContextApplication : Application() {
    override fun getApplicationContext(): Context {
        return this
    }

    override fun getSharedPreferences(name: String, mode: Int): SharedPreferences {
        return MockSharedPreferences()
    }

    override fun registerActivityLifecycleCallbacks(callback: ActivityLifecycleCallbacks) {
        //do nothing
    }
}
