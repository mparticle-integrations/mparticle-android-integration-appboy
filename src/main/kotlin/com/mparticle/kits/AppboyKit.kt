package com.mparticle.kits

import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.content.Context
import android.content.Intent
import android.os.Handler
import com.appboy.enums.Gender
import com.appboy.enums.Month
import com.appboy.enums.SdkFlavor
import com.braze.Braze
import com.braze.BrazeActivityLifecycleCallbackListener
import com.braze.BrazeUser
import com.braze.configuration.BrazeConfig
import com.braze.enums.BrazeSdkMetadata
import com.braze.models.outgoing.BrazeProperties
import com.braze.push.BrazeFirebaseMessagingService
import com.braze.push.BrazeNotificationUtils.isBrazePushMessage
import com.google.firebase.messaging.RemoteMessage
import com.mparticle.MPEvent
import com.mparticle.MParticle.IdentityType
import com.mparticle.MParticle.UserAttributes
import com.mparticle.commerce.CommerceEvent
import com.mparticle.commerce.Product
import com.mparticle.identity.MParticleUser
import com.mparticle.internal.Logger
import com.mparticle.kits.CommerceEventUtils.OnAttributeExtracted
import com.mparticle.kits.KitIntegration.*
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.*

/**
 * mParticle client-side Appboy integration
 */
open class AppboyKit : KitIntegration(), AttributeListener, CommerceListener,
    KitIntegration.EventListener, PushListener, IdentityListener {

    var enableTypeDetection = false
    var isMpidIdentityType = false
    var identityType: IdentityType? = null
    private val dataFlushHandler = Handler()
    private var dataFlushRunnable: Runnable? = null
    private var forwardScreenViews = false
    private var bundleNonPurchaseCommerceEvents = false
    private lateinit var updatedInstanceId: String


    override fun getName() = NAME


    public override fun onKitCreate(
        settings: Map<String, String?>,
        context: Context
    ): List<ReportingMessage>? {
        val key = settings[APPBOY_KEY]
        require(!KitUtils.isEmpty(key)) { "Braze key is empty." }

        //try to get endpoint from the host setting
        val authority = settings[HOST]
        if (!KitUtils.isEmpty(authority)) {
            setAuthority(authority)
        }
        val enableDetectionType = settings[ENABLE_TYPE_DETECTION]
        if (!KitUtils.isEmpty(enableDetectionType)) {
            try {
                enableTypeDetection = enableDetectionType.toBoolean()
            } catch (e: Exception) {
                Logger.warning("Braze, unable to parse \"enableDetectionType\"")
            }
        }
        forwardScreenViews = settings[FORWARD_SCREEN_VIEWS].toBoolean()
        bundleNonPurchaseCommerceEvents = settings[BUNDLE_NON_PURCHASE_COMMERCE_EVENTS].toBoolean()
        if (key != null) {
            val config = BrazeConfig.Builder().setApiKey(key)
                .setSdkFlavor(SdkFlavor.MPARTICLE)
                .setSdkMetadata(EnumSet.of(BrazeSdkMetadata.MPARTICLE))
                .build()
            Braze.configure(context, config)
        }
        dataFlushRunnable = Runnable {
            if (kitManager.isBackgrounded) {
                Braze.getInstance(getContext()).requestImmediateDataFlush()
            }
        }
        queueDataFlush()
        if (setDefaultAppboyLifecycleCallbackListener) {
            (context.applicationContext as Application).registerActivityLifecycleCallbacks(
                BrazeActivityLifecycleCallbackListener() as ActivityLifecycleCallbacks
            )
        }
        setIdentityType(settings)
        return null
    }

    fun setIdentityType(settings: Map<String, String?>) {
        val userIdentificationType = settings[USER_IDENTIFICATION_TYPE]
        if (!KitUtils.isEmpty(userIdentificationType)) {
            if (userIdentificationType == "MPID") {
                isMpidIdentityType = true
            } else {
                identityType = userIdentificationType?.let { IdentityType.valueOf(it) }
            }
        }
    }

    override fun setOptOut(optedOut: Boolean): List<ReportingMessage> = emptyList()

    override fun leaveBreadcrumb(breadcrumb: String): List<ReportingMessage> = emptyList()

    override fun logError(
        message: String,
        errorAttributes: Map<String, String>
    ): List<ReportingMessage> = emptyList()

    override fun logException(
        exception: Exception,
        exceptionAttributes: Map<String, String>,
        message: String
    ): List<ReportingMessage> = emptyList()

    override fun logEvent(event: MPEvent): List<ReportingMessage> {
        val newAttributes: MutableMap<String, Any?> = HashMap()
        if (event.customAttributes == null) {
            Braze.getInstance(context).logCustomEvent(event.eventName)
        } else {
            val properties = BrazeProperties()
            val user = Braze.getInstance(context).currentUser
            val brazePropertiesSetter = BrazePropertiesSetter(properties, enableTypeDetection)
            val userAttributeSetter = user?.let { UserAttributeSetter(it, enableTypeDetection) }
            event.customAttributeStrings?.let { it ->
                for ((key, value) in it) {
                    newAttributes[key] = brazePropertiesSetter.parseValue(key, value)
                    val hashedKey =
                        KitUtils.hashForFiltering(event.eventType.toString() + event.eventName + key)

                    configuration.eventAttributesAddToUser?.get(hashedKey)?.let {
                        user?.addToCustomAttributeArray(it, value)
                    }
                    configuration.eventAttributesRemoveFromUser?.get(hashedKey)?.let {
                        user?.removeFromCustomAttributeArray(it, value)
                    }
                    configuration.eventAttributesSingleItemUser?.get(hashedKey)?.let {
                        userAttributeSetter?.parseValue(it, value)
                    }
                }
            }
            Braze.getInstance(context).logCustomEvent(event.eventName, properties)
        }
        queueDataFlush()
        return listOf(ReportingMessage.fromEvent(this, event).setAttributes(newAttributes))
    }

    override fun logScreen(
        screenName: String,
        screenAttributes: Map<String, String>?
    ): List<ReportingMessage> {
        return if (forwardScreenViews) {
            if (screenAttributes == null) {
                Braze.getInstance(context).logCustomEvent(screenName)
            } else {
                val properties = BrazeProperties()
                val propertyParser = BrazePropertiesSetter(properties, enableTypeDetection)
                for ((key, value) in screenAttributes) {
                    propertyParser.parseValue(key, value)
                }
                Braze.getInstance(context).logCustomEvent(screenName, properties)
            }
            queueDataFlush()
            val messages: MutableList<ReportingMessage> = LinkedList()
            messages.add(
                ReportingMessage(
                    this,
                    ReportingMessage.MessageType.SCREEN_VIEW,
                    System.currentTimeMillis(),
                    screenAttributes
                )
            )
            messages
        } else {
            emptyList()
        }
    }

    override fun logLtvIncrease(
        valueIncreased: BigDecimal,
        valueTotal: BigDecimal,
        eventName: String,
        contextInfo: Map<String, String>
    ): List<ReportingMessage> = emptyList()

    override fun logEvent(event: CommerceEvent): List<ReportingMessage> {
        val messages: MutableList<ReportingMessage> = LinkedList()
        if (!KitUtils.isEmpty(event.productAction) &&
            event.productAction.equals(
                Product.PURCHASE,
                true
            ) && !event.products.isNullOrEmpty()
        ) {
            val productList = event.products
            productList?.let {
                for (product in productList) {
                    logTransaction(event, product)
                }
            }
            messages.add(ReportingMessage.fromEvent(this, event))
            queueDataFlush()
            return messages
        }
        val eventList = CommerceEventUtils.expand(event)
        if (eventList != null) {
            if (!bundleNonPurchaseCommerceEvents) {
                eventList.forEachIndexed { index, _ ->
                    try {
                        logEvent(eventList[index])
                        messages.add(ReportingMessage.fromEvent(this, event))
                    } catch (e: Exception) {
                        Logger.warning("Failed to call logCustomEvent to Braze kit: $e")
                    }
                }
            } else {
                val productArray = JSONArray()
                eventList.forEachIndexed { index, _ ->
                    val newAttributes = eventList[index].customAttributes ?: HashMap()
                    newAttributes["custom attributes"] = event.customAttributes
                    productArray.put(newAttributes)
                }
                try {
                    val json = JSONObject().put("products", productArray)
                    val transactionAttributes = event.transactionAttributes
                    transactionAttributes?.let {
                        json.put("Transaction ID",  transactionAttributes.id)
                    }
                    val brazeProperties = BrazeProperties(json)
                    Braze.getInstance(context).logCustomEvent(eventList[0].eventName, brazeProperties)
                    messages.add(ReportingMessage.fromEvent(this, event))
                } catch (jse: JSONException) {
                    Logger.warning("Failed to call logCustomEvent to Braze kit: $jse")
                }
            }
            queueDataFlush()
        }
        return messages
    }

    override fun setUserAttribute(keyIn: String, value: String) {
        var key = keyIn
        val user = Braze.getInstance(context).currentUser
        val userAttributeSetter = user?.let { UserAttributeSetter(it, enableTypeDetection) }

        user?.apply {
            when (key) {
                UserAttributes.CITY -> setHomeCity(value)
                UserAttributes.COUNTRY -> setCountry(value)
                UserAttributes.FIRSTNAME -> setFirstName(value)
                UserAttributes.LASTNAME -> setLastName(value)
                UserAttributes.MOBILE_NUMBER -> setPhoneNumber(value)
                UserAttributes.ZIPCODE -> setCustomUserAttribute("Zip", value)
                UserAttributes.AGE -> setDateOfBirth(key, value, user)
                "email_subscribe" -> setEmailSubscriptionStatus(key, value, user)
                "push_subscribe" -> setPushSubscriptionStatus(key, value, user)
                "dob" -> useDobString(value, user)
                UserAttributes.GENDER -> {
                    if (value.contains("fe")) setGender(Gender.FEMALE)
                    else setGender(Gender.MALE)
                }
                else -> {
                    if (key.startsWith("$")) {
                        key = key.substring(1)
                    }
                    userAttributeSetter?.parseValue(key, value)
                }
            }
        }
        queueDataFlush()
    }

    private fun setEmailSubscriptionStatus(key: String, value: String, user: BrazeUser){
        if (key == "email_subscribe") {
            when (value) {
                "opted_in" -> user.setEmailNotificationSubscriptionType(NotificationSubscriptionType.OPTED_IN)
                "unsubscribed" -> user.setEmailNotificationSubscriptionType(NotificationSubscriptionType.UNSUBSCRIBED)
                "subscribed" -> user.setEmailNotificationSubscriptionType(NotificationSubscriptionType.SUBSCRIBED)
                else -> {
                    Logger.error("unable to set email_subscribe with invalid value: " + value)
                }
            }
        }
    }

    private fun setPushSubscriptionStatus(key: String, value: String, user: BrazeUser){
        if (key == "push_subscribe") {
            when (value) {
                "opted_in" -> user.setPushNotificationSubscriptionType(NotificationSubscriptionType.OPTED_IN)
                "unsubscribed" -> user.setPushNotificationSubscriptionType(NotificationSubscriptionType.UNSUBSCRIBED)
                "subscribed" -> user.setPushNotificationSubscriptionType(NotificationSubscriptionType.SUBSCRIBED)
                else -> {
                    Logger.error("unable to set push_subscribe with invalid value: " + value)
                }
            }
        }
    }

    private fun setDateOfBirth(key: String, value: String, user: BrazeUser) {
        if (UserAttributes.AGE == key) {
            val calendar = getCalendarMinusYears(value)
            if (calendar != null) {
                user.setDateOfBirth(calendar[Calendar.YEAR], Month.JANUARY, 1)
            } else {
                Logger.error("unable to set DateOfBirth for " + UserAttributes.AGE + " = " + value)
            }
        }
    }

    // Expected Date Format @"yyyy'-'MM'-'dd"
    private fun useDobString(value: String, user: BrazeUser) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd")
        try {
            val calendar = Calendar.getInstance()
            calendar.time = dateFormat.parse(value) as Date
            val year = calendar[Calendar.YEAR]
            val monthNum = calendar[Calendar.MONTH]
            val month = Month.values()[monthNum]//
            val day = calendar[Calendar.DAY_OF_MONTH]
            user.setDateOfBirth(year, month, day)
        } catch (e: Exception) {
            Logger.error("unable to set DateOfBirth for \"dob\" = " + value + ". Exception: " + e.message)
        }
    }

    override fun setUserAttributeList(key: String, list: List<String>) {
        val user = Braze.getInstance(context).currentUser
        val array = list.toTypedArray<String?>()
        user?.setCustomAttributeArray(key, array)
        queueDataFlush()
    }

    override fun supportsAttributeLists(): Boolean = false

    protected open fun queueDataFlush() {
        dataFlushRunnable?.let { dataFlushHandler.removeCallbacks(it) }
        dataFlushRunnable?.let { dataFlushHandler.postDelayed(it, FLUSH_DELAY.toLong()) }
    }

    /**
     * This is called when the Kit is added to the mParticle SDK, typically on app-startup.
     */
    override fun setAllUserAttributes(
        attributes: Map<String, String>,
        attributeLists: Map<String, List<String>>
    ) {
        if (!kitPreferences.getBoolean(PREF_KEY_HAS_SYNCED_ATTRIBUTES, false)) {
            for ((key, value) in attributes) {
                setUserAttribute(key, value)
            }
            for ((key, value) in attributeLists) {
                setUserAttributeList(key, value)
            }
            kitPreferences.edit().putBoolean(PREF_KEY_HAS_SYNCED_ATTRIBUTES, true).apply()
        }
    }

    override fun removeUserAttribute(keyIn: String) {
        var key = keyIn
        val user = Braze.getInstance(context).currentUser

        if (UserAttributes.CITY == key) {
            user?.setHomeCity(null)
        } else if (UserAttributes.COUNTRY == key) {
            user?.setCountry(null)
        } else if (UserAttributes.FIRSTNAME == key) {
            user?.setFirstName(null)
        } //else if (UserAttributes.GENDER == key) {   //Braze SDK wont allow for gender parameter to be null.
        // user.setGender(null)}
        else if (UserAttributes.LASTNAME == key) {
            user?.setLastName(null)
        } else if (UserAttributes.MOBILE_NUMBER == key) {
            user?.setPhoneNumber(null)
        } else {
            if (key.startsWith("$")) {
                key = key.substring(1)
            }
            user?.unsetCustomUserAttribute(key)
        }
        queueDataFlush()
    }

    override fun setUserIdentity(identityType: IdentityType, identity: String) {}
    override fun removeUserIdentity(identityType: IdentityType) {}
    override fun logout(): List<ReportingMessage> = emptyList()

    fun logTransaction(event: CommerceEvent?, product: Product) {
        val purchaseProperties = BrazeProperties()
        val currency = arrayOfNulls<String>(1)
        val commerceTypeParser: StringTypeParser =
            BrazePropertiesSetter(purchaseProperties, enableTypeDetection)
        val onAttributeExtracted: OnAttributeExtracted = object : OnAttributeExtracted {
            override fun onAttributeExtracted(key: String, value: String) {
                if (!checkCurrency(key, value)) {
                    commerceTypeParser.parseValue(key, value)
                }
            }

            override fun onAttributeExtracted(key: String, value: Double) {
                if (!checkCurrency(key, value)) {
                    purchaseProperties.addProperty(key, value)
                }
            }

            override fun onAttributeExtracted(key: String, value: Int) {
                purchaseProperties.addProperty(key, value)
            }

            override fun onAttributeExtracted(attributes: Map<String, String>) {
                for ((key, value) in attributes) {
                    if (!checkCurrency(key, value)) {
                        commerceTypeParser.parseValue(key, value)
                    }
                }
            }

            private fun checkCurrency(key: String, value: Any?): Boolean {
                return if (CommerceEventUtils.Constants.ATT_ACTION_CURRENCY_CODE == key) {
                    currency[0] = value?.toString()
                    true
                } else {
                    false
                }
            }
        }
        CommerceEventUtils.extractActionAttributes(event, onAttributeExtracted)
        purchaseProperties.addProperty("custom_attributes", event?.customAttributes ?: "")
        var currencyValue = currency[0]
        if (KitUtils.isEmpty(currencyValue)) {
            currencyValue = CommerceEventUtils.Constants.DEFAULT_CURRENCY_CODE
        }
        Braze.Companion.getInstance(context).logPurchase(
            product.sku,
            currencyValue,
            BigDecimal(product.unitPrice),
            product.quantity.toInt(),
            purchaseProperties
        )
    }

    override fun willHandlePushMessage(intent: Intent): Boolean {
        return if ((settings[PUSH_ENABLED]).toBoolean()) {
            false
        } else intent.isBrazePushMessage()
    }

    override fun onPushMessageReceived(context: Context, pushIntent: Intent) {
        if (settings[PUSH_ENABLED].toBoolean()) {
            BrazeFirebaseMessagingService.handleBrazeRemoteMessage(
                context,
                RemoteMessage(pushIntent.extras)
            )
        }
    }

    override fun onPushRegistration(instanceId: String, senderId: String): Boolean {
        return if (settings[PUSH_ENABLED].toBoolean()) {
            updatedInstanceId = instanceId
            Braze.getInstance(context).registeredPushToken
            queueDataFlush()
            true
        } else {
            false
        }
    }

    protected open fun setAuthority(authority: String?) {
        Braze.setEndpointProvider { appboyEndpoint ->
            appboyEndpoint.buildUpon()
                .authority(authority).build()
        }
    }

    override fun onIdentifyCompleted(
        mParticleUser: MParticleUser,
        filteredIdentityApiRequest: FilteredIdentityApiRequest?
    ) {
        updateUser(mParticleUser)
    }

    override fun onLoginCompleted(
        mParticleUser: MParticleUser,
        filteredIdentityApiRequest: FilteredIdentityApiRequest?
    ) {
        updateUser(mParticleUser)
    }

    override fun onLogoutCompleted(
        mParticleUser: MParticleUser,
        filteredIdentityApiRequest: FilteredIdentityApiRequest?
    ) {
        updateUser(mParticleUser)
    }

    override fun onModifyCompleted(
        mParticleUser: MParticleUser,
        filteredIdentityApiRequest: FilteredIdentityApiRequest?
    ) {
        updateUser(mParticleUser)
    }

    private fun updateUser(mParticleUser: MParticleUser) {
        val identity = getIdentity(isMpidIdentityType, identityType, mParticleUser)
        val email = mParticleUser.userIdentities[IdentityType.Email]
        identity?.let { setId(it) }
        email?.let { setEmail(it) }
    }

    fun getIdentity(
        isMpidIdentityType: Boolean,
        identityType: IdentityType?,
        mParticleUser: MParticleUser?
    ): String? {
        var identity: String? = null
        if (isMpidIdentityType && mParticleUser != null) {
            identity = mParticleUser.id.toString()

        } else if (identityType != null && mParticleUser != null) {
            identity = mParticleUser.userIdentities[identityType]
        }
        return identity
    }

    protected open fun setId(customerId: String) {
        val user = Braze.getInstance(context).currentUser
        if (user == null || user.userId != customerId) {
            Braze.getInstance(context).changeUser(customerId)
            queueDataFlush()
        }
    }

    protected open fun setEmail(email: String) {
        if (email != kitPreferences.getString(PREF_KEY_CURRENT_EMAIL, null)) {
            val user = Braze.getInstance(context).currentUser
            user?.setEmail(email)
            queueDataFlush()
            kitPreferences.edit().putString(PREF_KEY_CURRENT_EMAIL, email).apply()
        }
    }

    override fun onUserIdentified(mParticleUser: MParticleUser) {
        if (updatedInstanceId.isNotEmpty()) {
            Braze.getInstance(context).registeredPushToken
        }
    }

    fun addToProperties(properties: BrazeProperties, key: String, value: String) {
        try {
            if ("true".equals(value, true) || "false".equals(
                    value,
                    true
                )
            ) {
                properties.addProperty(key, (value).toBoolean())
            } else {
                val doubleValue = value.toDouble()
                if (doubleValue % 1 == 0.0) {
                    properties.addProperty(key, value.toInt())
                } else {
                    properties.addProperty(key, doubleValue)
                }
            }
        } catch (e: Exception) {
            properties.addProperty(key, value)
        }
    }

    fun getCalendarMinusYears(yearsString: String): Calendar? {
        try {
            val years = yearsString.toInt()
            return getCalendarMinusYears(years)
        } catch (ignored: NumberFormatException) {
            try {
                val years = yearsString.toDouble()
                return getCalendarMinusYears(years.toInt())
            } catch (ignoredToo: NumberFormatException) {
            }
        }
        return null
    }

    fun getCalendarMinusYears(years: Int): Calendar? {
        return if (years >= 0) {
            val calendar = Calendar.getInstance()
            calendar[Calendar.YEAR] = calendar[Calendar.YEAR] - years
            calendar
        } else {
            null
        }
    }

    internal abstract class StringTypeParser(var enableTypeDetection: Boolean) {
        fun parseValue(key: String, value: String): Any {
            if (!enableTypeDetection) {
                toString(key, value)
                return value
            }
            return if (true.toString().equals(value, true) || false.toString().equals(
                    value,
                    true
                )
            ) {
                val newBool = (value).toBoolean()
                toBoolean(key, newBool)
                newBool
            } else {
                try {
                    if (value.contains(".")) {
                        val doubleValue = value.toDouble()
                        toDouble(key, doubleValue)
                        doubleValue
                    } else {
                        val newLong = value.toLong()
                        if (newLong <= Int.MAX_VALUE && newLong >= Int.MIN_VALUE) {
                            val newInt = newLong.toInt()
                            toInt(key, newInt)
                            newInt
                        } else {
                            toLong(key, newLong)
                            newLong
                        }
                    }
                } catch (nfe: NumberFormatException) {
                    toString(key, value)
                    value
                }
            }
        }

        abstract fun toInt(key: String, value: Int)
        abstract fun toLong(key: String, value: Long)
        abstract fun toDouble(key: String, value: Double)
        abstract fun toBoolean(key: String, value: Boolean)
        abstract fun toString(key: String, value: String)
    }

    internal inner class BrazePropertiesSetter(
        private var properties: BrazeProperties,
        enableTypeDetection: Boolean
    ) : StringTypeParser(enableTypeDetection) {
        override fun toInt(key: String, value: Int) {
            properties.addProperty(key, value)
        }

        override fun toLong(key: String, value: Long) {
            properties.addProperty(key, value)
        }

        override fun toDouble(key: String, value: Double) {
            properties.addProperty(key, value)
        }

        override fun toBoolean(key: String, value: Boolean) {
            properties.addProperty(key, value)
        }

        override fun toString(key: String, value: String) {
            properties.addProperty(key, value)
        }
    }

    internal inner class UserAttributeSetter(
        private var brazeUser: BrazeUser,
        enableTypeDetection: Boolean
    ) : StringTypeParser(enableTypeDetection) {
        override fun toInt(key: String, value: Int) {
            brazeUser.setCustomUserAttribute(key, value)
        }

        override fun toLong(key: String, value: Long) {
            brazeUser.setCustomUserAttribute(key, value)
        }

        override fun toDouble(key: String, value: Double) {
            brazeUser.setCustomUserAttribute(key, value)
        }

        override fun toBoolean(key: String, value: Boolean) {
            brazeUser.setCustomUserAttribute(key, value)
        }

        override fun toString(key: String, value: String) {
            brazeUser.setCustomUserAttribute(key, value)
        }
    }

    companion object {
        const val APPBOY_KEY = "apiKey"
        const val FORWARD_SCREEN_VIEWS = "forwardScreenViews"
        const val BUNDLE_NON_PURCHASE_COMMERCE_EVENTS = "bundleNonPurchaseCommerceEvents"
        const val USER_IDENTIFICATION_TYPE = "userIdentificationType"
        const val ENABLE_TYPE_DETECTION = "enableTypeDetection"
        const val HOST = "host"
        const val PUSH_ENABLED = "push_enabled"
        const val NAME = "Appboy"
        private const val PREF_KEY_HAS_SYNCED_ATTRIBUTES = "appboy::has_synced_attributes"
        private const val PREF_KEY_CURRENT_EMAIL = "appboy::current_email"
        private const val FLUSH_DELAY = 5000
        var setDefaultAppboyLifecycleCallbackListener = true
    }
}
