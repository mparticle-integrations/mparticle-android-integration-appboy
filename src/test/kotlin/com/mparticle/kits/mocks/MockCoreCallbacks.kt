package com.mparticle.kits.mocks

import com.mparticle.internal.CoreCallbacks
import java.lang.ref.WeakReference
import android.app.Activity
import com.mparticle.MParticleOptions.DataplanOptions
import android.net.Uri
import com.mparticle.internal.CoreCallbacks.KitListener
import org.json.JSONArray

class MockCoreCallbacks : CoreCallbacks {
    override fun isBackgrounded(): Boolean {
        return false
    }

    override fun getUserBucket(): Int {
        return 0
    }

    override fun isEnabled(): Boolean {
        return false
    }

    override fun setIntegrationAttributes(kitId: Int, integrationAttributes: Map<String, String>) {}
    override fun getIntegrationAttributes(kitId: Int): Map<String, String>? {
        return null
    }

    override fun getCurrentActivity(): WeakReference<Activity>? {
        return null
    }

    override fun getLatestKitConfiguration(): JSONArray? {
        return null
    }

    override fun getDataplanOptions(): DataplanOptions? {
        return null
    }

    override fun isPushEnabled(): Boolean {
        return false
    }

    override fun getPushSenderId(): String? {
        return null
    }

    override fun getPushInstanceId(): String? {
        return null
    }

    override fun getLaunchUri(): Uri? {
        return null
    }

    override fun getLaunchAction(): String? {
        return null
    }

    override fun getKitListener(): KitListener {
        return object : KitListener {
            override fun kitFound(kitId: Int) {}
            override fun kitConfigReceived(kitId: Int, configuration: String?) {}
            override fun kitExcluded(kitId: Int, reason: String?) {}
            override fun kitStarted(kitId: Int) {}
            override fun onKitApiCalled(kitId: Int, used: Boolean?, vararg objects: Any?) {}
            override fun onKitApiCalled(methodName: String?, kitId: Int, used: Boolean?, vararg objects: Any?) {}
        }
    }
}
