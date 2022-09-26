package com.braze

import android.content.Context
import com.braze.configuration.BrazeConfig
import com.braze.models.outgoing.BrazeProperties
import com.mparticle.kits.BrazePurchase
import java.math.BigDecimal

class Braze {

    fun getCurrentUser(): BrazeUser {
        return Companion.currentUser
    }

    fun logCustomEvent(key: String, brazeProperties: BrazeProperties) {
        events[key] = brazeProperties
    }

    fun logPurchase(
        sku: String,
        currency: String,
        unitPrice: BigDecimal,
        quantity: Int,
        purchaseProperties: BrazeProperties
    ) {
        purchases.add(BrazePurchase(sku, currency, unitPrice, quantity, purchaseProperties))
    }

    companion object {
        val purchases: MutableList<BrazePurchase> = ArrayList()
        val events: MutableMap<String, BrazeProperties> = HashMap()

        fun clearPurchases() {
            purchases.clear()
        }

        fun clearEvents() {
            events.clear()
        }

        val currentUser = BrazeUser()

        @JvmStatic
        fun configure(context: Context?, config: BrazeConfig?) = true

        @JvmStatic
        fun getInstance(context: Context?): Braze = Braze()
    }
}
