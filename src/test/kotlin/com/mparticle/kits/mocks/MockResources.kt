package com.mparticle.kits.mocks

import android.content.res.Resources
import kotlin.Throws
import android.content.res.Resources.NotFoundException
import com.mparticle.kits.mocks.MockResources

class MockResources : Resources(null, null, null) {
    override fun getIdentifier(name: String, defType: String, defPackage: String): Int {
        if (name == "mp_key") {
            return 1
        } else if (name == "mp_secret") {
            return 2
        }
        return 0
    }

    @Throws(NotFoundException::class)
    override fun getString(id: Int): String {
        when (id) {
            1 -> return TEST_APP_KEY
            2 -> return TEST_APP_SECRET
        }
        return ""
    }

    @Throws(NotFoundException::class)
    override fun getString(id: Int, vararg formatArgs: Any): String {
        return super.getString(id, *formatArgs)
    }

    companion object {
        var TEST_APP_KEY = "the app key"
        var TEST_APP_SECRET = "the app secret"
    }
}
