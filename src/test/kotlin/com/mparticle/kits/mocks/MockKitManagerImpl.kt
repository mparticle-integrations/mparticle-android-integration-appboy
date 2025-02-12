package com.mparticle.kits.mocks

import android.content.Context
import com.mparticle.internal.ReportingManager
import com.mparticle.internal.CoreCallbacks
import com.mparticle.kits.KitManagerImpl
import org.mockito.Mockito
import com.mparticle.MParticleOptions
import com.mparticle.kits.mocks.MockContext
import com.mparticle.internal.CoreCallbacks.KitListener
import kotlin.Throws
import com.mparticle.kits.KitConfiguration
import com.mparticle.kits.mocks.MockKitConfiguration
import org.json.JSONException
import org.json.JSONObject

class MockKitManagerImpl(
    context: Context?,
    reportingManager: ReportingManager?,
    coreCallbacks: CoreCallbacks?
) : KitManagerImpl(
    context, reportingManager, coreCallbacks, Mockito.mock(
        MParticleOptions::class.java
    )
) {
    constructor() : this(
        MockContext(),
        Mockito.mock<ReportingManager>(ReportingManager::class.java),
        Mockito.mock<CoreCallbacks>(
            CoreCallbacks::class.java
        )
    ) {
        Mockito.`when`(mCoreCallbacks.getKitListener()).thenReturn(KitListener.EMPTY)
    }

    @Throws(JSONException::class)
    override fun createKitConfiguration(configuration: JSONObject): KitConfiguration {
        return MockKitConfiguration.createKitConfiguration(configuration)
    }

    override fun getUserBucket(): Int {
        return 50
    }
}
