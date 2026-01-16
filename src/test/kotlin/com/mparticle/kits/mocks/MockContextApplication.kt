package com.mparticle.kits.mocks

import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.content.Context
import android.content.SharedPreferences
import com.mparticle.kits.mocks.MockSharedPreferences

class MockContextApplication : Application() {
    override fun getApplicationContext(): Context = this

    override fun getSharedPreferences(name: String, mode: Int): SharedPreferences = MockSharedPreferences()

    override fun registerActivityLifecycleCallbacks(callback: ActivityLifecycleCallbacks) {
        // do nothing
    }
}
