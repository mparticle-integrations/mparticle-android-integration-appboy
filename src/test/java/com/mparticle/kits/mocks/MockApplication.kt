package com.mparticle.kits.mocks

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.res.Resources

class MockApplication(var mContext: MockContext) : Application() {
    var mCallbacks: ActivityLifecycleCallbacks? = null
    override fun registerActivityLifecycleCallbacks(callback: ActivityLifecycleCallbacks) {
        mCallbacks = callback
    }

    override fun getApplicationContext(): Context {
        return this
    }

    fun setSharedPreferences(prefs: SharedPreferences) {
        mContext.setSharedPreferences(prefs)
    }

    override fun getSystemService(name: String): Any? {
        return mContext.getSystemService(name)
    }

    override fun getSharedPreferences(name: String, mode: Int): SharedPreferences {
        return mContext.getSharedPreferences(name, mode)
    }

    override fun getPackageManager(): PackageManager {
        return mContext.packageManager
    }

    override fun getPackageName(): String {
        return mContext.packageName
    }

    override fun getApplicationInfo(): ApplicationInfo {
        return mContext.applicationInfo
    }

    override fun getResources(): Resources? {
        return mContext.resources
    }
}
