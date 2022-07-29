package com.braze

import android.content.Context
import com.mparticle.kits.BrazePurchase
import java.util.ArrayList
import java.util.HashMap
import com.braze.configuration.BrazeConfig
import com.braze.models.outgoing.BrazeProperties
import java.math.BigDecimal

object Braze {

     val currentUser = BrazeUser()
     val purchases: MutableList<BrazePurchase> = ArrayList()
     val events: MutableMap<String, BrazeProperties> = HashMap()

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

    fun clearPurchases() {
        purchases.clear()
    }

    fun clearEvents() {
        events.clear()
    }

    @JvmStatic
    fun configure(context: Context?, config: BrazeConfig?) = true


    @JvmStatic
    //Mocks getInstance method in actual Braze lib.
    fun getInstance(context: Context?): Braze = this

}
