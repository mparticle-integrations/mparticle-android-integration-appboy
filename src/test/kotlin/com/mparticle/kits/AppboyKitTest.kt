package com.mparticle.kits

import com.appboy.enums.Month
import com.braze.Braze
import com.mparticle.MPEvent
import com.mparticle.MParticle
import com.mparticle.MParticle.IdentityType
import com.mparticle.MParticle.LogLevel
import com.mparticle.commerce.CommerceEvent
import com.mparticle.commerce.Product
import com.mparticle.commerce.TransactionAttributes
import com.mparticle.identity.IdentityApi
import com.mparticle.identity.MParticleUser
import com.mparticle.internal.Logger
import com.mparticle.internal.Logger.DefaultLogHandler
import com.mparticle.kits.mocks.MockAppboyKit
import com.mparticle.kits.mocks.MockContextApplication
import com.mparticle.kits.mocks.MockKitConfiguration
import com.mparticle.kits.mocks.MockUser
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import java.math.BigDecimal
import java.util.Calendar
import java.util.Locale
import java.util.Random

class AppboyKitTests {
    private var random = Random()
    private val kit: AppboyKit
        get() = AppboyKit()

    @Before
    fun setup() {
        MParticle.setInstance(Mockito.mock(MParticle::class.java))
        Mockito.`when`(MParticle.getInstance()!!.Identity()).thenReturn(
            Mockito.mock(
                IdentityApi::class.java
            )
        )
        Braze.clearPurchases()
        Braze.clearEvents()
    }

    @Test
    @Throws(Exception::class)
    fun testGetName() {
        val name = kit.name
        Assert.assertTrue(name.isNotEmpty())
    }

    /**
     * Kit *should* throw an exception when they're initialized with the wrong settings.
     */
    @Test
    @Throws(Exception::class)
    fun testOnKitCreate() {
        var e: Exception? = null
        try {
            val kit: KitIntegration = kit
            val settings = HashMap<String, String>()
            settings["fake setting"] = "fake"
            kit.onKitCreate(settings, MockContextApplication())
        } catch (ex: Exception) {
            e = ex
        }
        Assert.assertNotNull(e)
    }

    @Test
    @Throws(Exception::class)
    fun testClassName() {
        val factory = KitIntegrationFactory()
        val integrations = factory.knownIntegrations
        val className = kit.javaClass.name
        for (integration in integrations) {
            if (integration.value == className) {
                return
            }
        }
        Assert.fail("$className not found as a known integration.")
    }

    private var hostName = "aRandomHost"

    @Test
    @Throws(Exception::class)
    fun testHostSetting() {
        val settings = HashMap<String, String>()
        settings[AppboyKit.HOST] = hostName
        settings[AppboyKit.APPBOY_KEY] = "key"
        val kit = MockAppboyKit()
        kit.onKitCreate(settings, MockContextApplication())
        Assert.assertTrue(kit.calledAuthority[0] == hostName)
    }

    @Test
    @Throws(Exception::class)
    fun testHostSettingNull() {
        //test that the key is set when it is passed in by the settings map
        val missingSettings = HashMap<String, String>()
        missingSettings[AppboyKit.APPBOY_KEY] = "key"
        val kit = MockAppboyKit()
        try {
            kit.onKitCreate(missingSettings, MockContextApplication())
        } catch (e: Exception) {
        }
        Assert.assertTrue(kit.calledAuthority[0] == null)
    }

    @Test
    @Throws(Exception::class)
    fun testHostSettingEmpty() {
        var nullSettings = HashMap<String, String?>()
        nullSettings[AppboyKit.HOST] = null
        nullSettings[AppboyKit.APPBOY_KEY] = "key"
        var kit = MockAppboyKit()
        try {
            kit.onKitCreate(nullSettings, MockContextApplication())
        } catch (e: Exception) {
        }
        Assert.assertTrue(kit.calledAuthority[0] == null)
        nullSettings = HashMap()
        nullSettings[AppboyKit.HOST] = ""
        nullSettings[AppboyKit.APPBOY_KEY] = "key"
        kit = MockAppboyKit()
        try {
            kit.onKitCreate(nullSettings, MockContextApplication())
        } catch (e: Exception) {
        }
        Assert.assertTrue(kit.calledAuthority[0] == null)
    }

    @Test
    fun testOnModify() {
        //make sure it doesn't crash if there is no email or customerId
        var e: Exception? = null
        try {
            AppboyKit().onModifyCompleted(MockUser(HashMap()), null)
        } catch (ex: Exception) {
            e = ex
        }
        Assert.assertNull(e)
        for (i in 0..3) {
            val values = arrayOfNulls<String>(2)
            val mockEmail = "mockEmail$i"
            val mockCustomerId = "12345$i"
            val kit: AppboyKit = object : AppboyKit() {
                override fun setId(customerId: String) {
                    values[0] = customerId
                }

                override fun setEmail(email: String) {
                    if (values[0] == null) {
                        Assert.fail("customerId should have been set first")
                    }
                    values[1] = email
                }
            }
            kit.identityType = IdentityType.CustomerId
            val map = HashMap<IdentityType, String>()
            map[IdentityType.Email] = mockEmail
            map[IdentityType.Alias] = "alias"
            map[IdentityType.Facebook] = "facebook"
            map[IdentityType.Facebook] = "fb"
            map[IdentityType.CustomerId] = mockCustomerId
            when (i) {
                0 -> {
                    kit.onModifyCompleted(MockUser(map), null)
                    kit.onIdentifyCompleted(MockUser(map), null)
                    kit.onLoginCompleted(MockUser(map), null)
                    kit.onLogoutCompleted(MockUser(map), null)
                }
                1 -> {
                    kit.onIdentifyCompleted(MockUser(map), null)
                    kit.onLoginCompleted(MockUser(map), null)
                    kit.onLogoutCompleted(MockUser(map), null)
                }
                2 -> {
                    kit.onLoginCompleted(MockUser(map), null)
                    kit.onLogoutCompleted(MockUser(map), null)
                }
                3 -> kit.onLogoutCompleted(MockUser(map), null)
            }
            Assert.assertEquals(mockCustomerId, values[0])
            Assert.assertEquals(mockEmail, values[1])
        }
    }

    @Test
    fun testAgeToDob() {
        val kit: AppboyKit = MockAppboyKit()
        val currentYear = Calendar.getInstance()[Calendar.YEAR]
        var calendar = kit.getCalendarMinusYears("5")
        calendar?.get(Calendar.YEAR)
            ?.let { Assert.assertEquals((currentYear - 5).toLong(), it.toLong()) }
        calendar = kit.getCalendarMinusYears(22)
        calendar?.get(Calendar.YEAR)
            ?.let { Assert.assertEquals((currentYear - 22).toLong(), it.toLong()) }

//        round down doubles
        calendar = kit.getCalendarMinusYears("5.001")
        calendar?.get(Calendar.YEAR)
            ?.let { Assert.assertEquals((currentYear - 5).toLong(), it.toLong()) }
        calendar = kit.getCalendarMinusYears("5.9")
        calendar?.get(Calendar.YEAR)
            ?.let { Assert.assertEquals((currentYear - 5).toLong(), it.toLong()) }

        //invalid ages (negative, non numeric), don't get set
        Assert.assertNull(kit.getCalendarMinusYears("asdv"))
        Assert.assertNull(kit.getCalendarMinusYears(-1))
    }

    @Test
    fun testSetUserAttributeAge() {
        val currentYear = Calendar.getInstance()[Calendar.YEAR]
        val kit: AppboyKit = MockAppboyKit()
        val currentUser = Braze.currentUser
        Assert.assertEquals(-1, currentUser.dobDay.toLong())
        Assert.assertEquals(-1, currentUser.dobYear.toLong())
        Assert.assertNull(currentUser.dobMonth)
        kit.setUserAttribute(MParticle.UserAttributes.AGE, "100")
        Assert.assertEquals((currentYear - 100).toLong(), currentUser.dobYear.toLong())
        Assert.assertEquals(1, currentUser.dobDay.toLong())
        Assert.assertEquals(Month.JANUARY, currentUser.dobMonth)
    }

    @Test
    fun testSetUserDoB() {
        val kit = MockAppboyKit()
        val currentUser = Braze.currentUser
        val errorMessage = arrayOfNulls<String>(1)
        Logger.setLogHandler(object : DefaultLogHandler() {
            override fun log(priority: LogLevel, error: Throwable?, messages: String) {
                if (priority == LogLevel.ERROR) {
                    errorMessage[0] = messages
                }
            }
        })

        //valid
        kit.setUserAttribute("dob", "1999-11-05")
        Assert.assertEquals(1999, currentUser.dobYear.toLong())
        Assert.assertEquals(5, currentUser.dobDay.toLong())
        Assert.assertEquals(Month.NOVEMBER, currentUser.dobMonth)
        Assert.assertNull(errorMessage[0])

        //future
        kit.setUserAttribute("dob", "2999-2-15")
        Assert.assertEquals(2999, currentUser.dobYear.toLong())
        Assert.assertEquals(15, currentUser.dobDay.toLong())
        Assert.assertEquals(Month.FEBRUARY, currentUser.dobMonth)
        Assert.assertNull(errorMessage[0])


        //bad format (shouldn't crash, but should message)
        var ex: Exception? = null
        try {
            kit.setUserAttribute("dob", "2kjb.21h045")
            Assert.assertEquals(2999, currentUser.dobYear.toLong())
            Assert.assertEquals(15, currentUser.dobDay.toLong())
            Assert.assertEquals(Month.FEBRUARY, currentUser.dobMonth)
            Assert.assertNotNull(errorMessage[0])
        } catch (e: Exception) {
            ex = e
        }
        Assert.assertNull(ex)
    }

    @Test
    fun setIdentityType() {
        val possibleValues = arrayOf(
            "Other", "CustomerId", "Facebook",
            "Twitter", "Google", "Microsoft",
            "Yahoo", "Email", "Alias"
        )
        val mpid = "MPID"
        for (`val` in possibleValues) {
            val kit = kit
            val settings = HashMap<String, String>()
            settings[AppboyKit.USER_IDENTIFICATION_TYPE] = `val`
            kit.setIdentityType(settings)
            Assert.assertNotNull(kit.identityType)
            Assert.assertEquals(
                `val`.lowercase(Locale.getDefault()),
                kit.identityType?.name?.lowercase(Locale.getDefault())
            )
            Assert.assertFalse(kit.isMpidIdentityType)
        }
        val settings = HashMap<String, String>()
        settings[AppboyKit.USER_IDENTIFICATION_TYPE] = mpid
        val kit = kit
        kit.setIdentityType(settings)
        Assert.assertNull(kit.identityType)
        Assert.assertTrue(kit.isMpidIdentityType)
    }

    @Test
    fun setId() {
        val userIdentities = HashMap<IdentityType, String>()
        val user = Mockito.mock(MParticleUser::class.java)
        Mockito.`when`(user.userIdentities).thenReturn(userIdentities)
        val mockId = random.nextLong()
        Mockito.`when`(user.id).thenReturn(mockId)
        Assert.assertEquals(mockId.toString(), kit.getIdentity(true, null, user))
        for (identityType in IdentityType.values()) {
            val identityValue = random.nextLong().toString()
            userIdentities[identityType] = identityValue
            Assert.assertEquals(identityValue, kit.getIdentity(false, identityType, user))
        }
        Assert.assertNull(kit.getIdentity(false, null, null))
    }

    @Test
    fun addRemoveAttributeFromEventTest() {
        val kit = MockAppboyKit()
        val currentUser = Braze.currentUser
        kit.configuration = object : MockKitConfiguration() {

            override fun getEventAttributesAddToUser(): Map<Int, String> {
                val map = HashMap<Int, String>()
                map[KitUtils.hashForFiltering(MParticle.EventType.Navigation.toString() + "Navigation Event" + "key1")] =
                    "output"
                return map
            }

            override fun getEventAttributesRemoveFromUser(): Map<Int, String> {
                val map = HashMap<Int, String>()
                map[KitUtils.hashForFiltering(MParticle.EventType.Location.toString() + "location event" + "key1")] =
                    "output"
                return map
            }

        }
        val customAttributes = HashMap<String, String>()
        customAttributes["key1"] = "value1"
        kit.logEvent(
            MPEvent.Builder("Navigation Event", MParticle.EventType.Navigation)
                .customAttributes(customAttributes)
                .build()
        )
        var attributes = currentUser.customAttributeArray["output"]
        if (attributes != null) {
            Assert.assertEquals(1, attributes.size)
            Assert.assertEquals("value1", attributes[0])
        }
        kit.logEvent(
            MPEvent.Builder("location event", MParticle.EventType.Location)
                .customAttributes(customAttributes)
                .build()
        )
        attributes = currentUser.customAttributeArray["output"]

        if (attributes != null) {
            Assert.assertEquals(0, attributes.size)
        }
    }

    @Test
    fun testPurchaseCurrency() {
        val kit = MockAppboyKit()
        val product = Product.Builder("product name", "sku1", 4.5)
            .build()
        val commerceEvent = CommerceEvent.Builder(Product.CHECKOUT, product)
            .currency("Moon Dollars")
            .build()
        kit.logTransaction(commerceEvent, product)
        val braze = Braze
        val purchases = braze.purchases
        Assert.assertEquals(1, purchases.size.toLong())
        val purchase = purchases[0]
        Assert.assertEquals("Moon Dollars", purchase.currency)
        Assert.assertNull(purchase.purchaseProperties.properties[CommerceEventUtils.Constants.ATT_ACTION_CURRENCY_CODE])
    }

    @Test
    fun testPurchaseDefaultCurrency() {
        val kit = MockAppboyKit()
        val product = Product.Builder("product name", "sku1", 4.5)
            .build()
        val commerceEvent = CommerceEvent.Builder(Product.CHECKOUT, product)
            .build()
        kit.logTransaction(commerceEvent, product)
        val braze = Braze
        val purchases = braze.purchases
        Assert.assertEquals(1, purchases.size.toLong())
        val purchase = purchases[0]
        Assert.assertEquals(CommerceEventUtils.Constants.DEFAULT_CURRENCY_CODE, purchase.currency)
        Assert.assertNull(purchase.purchaseProperties.properties[CommerceEventUtils.Constants.ATT_ACTION_CURRENCY_CODE])
    }

    @Test
    fun testPurchase() {
        val kit = MockAppboyKit()
        val customAttributes = HashMap<String, String>()
        customAttributes["key1"] = "value1"
        customAttributes["key #2"] = "value #3"
        val transactionAttributes = TransactionAttributes("the id")
            .setTax(100.0)
            .setShipping(12.0)
            .setRevenue(99.0)
            .setCouponCode("coupon code")
            .setAffiliation("the affiliation")
        val product = Product.Builder("product name", "sku1", 4.5)
            .quantity(5.0)
            .build()
        val commerceEvent = CommerceEvent.Builder(Product.CHECKOUT, product)
            .currency("Moon Dollars")
            .productListName("product list name")
            .productListSource("the source")
            .customAttributes(customAttributes)
            .transactionAttributes(transactionAttributes)
            .build()
        kit.logTransaction(commerceEvent, product)
        val braze = Braze
        val purchases = braze.purchases
        Assert.assertEquals(1, purchases.size.toLong())
        val purchase = purchases[0]
        Assert.assertEquals("Moon Dollars", purchase.currency)
        Assert.assertEquals(5.0, purchase.quantity.toDouble(), 0.01)
        Assert.assertEquals("sku1", purchase.sku)
        Assert.assertEquals(BigDecimal(4.5), purchase.unitPrice)
        Assert.assertNotNull(purchase.purchaseProperties)
        val properties = purchase.purchaseProperties.properties
        Assert.assertEquals(properties.remove(CommerceEventUtils.Constants.ATT_SHIPPING), 12.0)
        Assert.assertEquals(
            properties.remove(CommerceEventUtils.Constants.ATT_ACTION_PRODUCT_LIST_SOURCE),
            "the source"
        )
        Assert.assertEquals(properties.remove(CommerceEventUtils.Constants.ATT_TAX), 100.0)
        Assert.assertEquals(properties.remove(CommerceEventUtils.Constants.ATT_TOTAL), 99.0)
        Assert.assertEquals(
            properties.remove(CommerceEventUtils.Constants.ATT_ACTION_PRODUCT_ACTION_LIST),
            "product list name"
        )
        Assert.assertEquals(
            properties.remove(CommerceEventUtils.Constants.ATT_PRODUCT_COUPON_CODE),
            "coupon code"
        )
        Assert.assertEquals(
            properties.remove(CommerceEventUtils.Constants.ATT_TRANSACTION_ID),
            "the id"
        )
        Assert.assertEquals(
            properties.remove(CommerceEventUtils.Constants.ATT_AFFILIATION),
            "the affiliation"
        )
        Assert.assertEquals(1, properties.size.toLong())
    }

    @Test
    fun setUserAttributeTyped() {
        val kit = MockAppboyKit()
        kit.enableTypeDetection = true
        val currentUser = Braze.currentUser
        kit.setUserAttribute("foo", "true")
        Assert.assertTrue(currentUser.customUserAttributes["foo"] is Boolean)
        Assert.assertEquals(currentUser.customUserAttributes["foo"], true)
        kit.setUserAttribute("foo", "1")
        Assert.assertTrue(currentUser.customUserAttributes["foo"] is Int)
        Assert.assertEquals(currentUser.customUserAttributes["foo"], 1)
        kit.setUserAttribute("foo", "1.1")
        Assert.assertTrue(currentUser.customUserAttributes["foo"] is Double)
        Assert.assertEquals(currentUser.customUserAttributes["foo"], 1.1)
        kit.setUserAttribute("foo", "bar")
        Assert.assertTrue(currentUser.customUserAttributes["foo"] is String)
        Assert.assertEquals(currentUser.customUserAttributes["foo"], "bar")
    }

    @Test
    fun testEventStringType() {
        val kit = MockAppboyKit()
        kit.configuration = MockKitConfiguration()
        val customAttributes = HashMap<String, String?>()
        customAttributes["foo"] = "false"
        customAttributes["bar"] = "1"
        customAttributes["baz"] = "1.5"
        customAttributes["fuzz?"] = "foobar"
        val customEvent = MPEvent.Builder("testEvent", MParticle.EventType.Location)
            .customAttributes(customAttributes)
            .build()
        kit.enableTypeDetection = true
        kit.logEvent(customEvent)
        val braze = Braze
        val events = braze.events
        Assert.assertEquals(1, events.values.size.toLong())
        val event = events.values.iterator().next()
        val properties = event.properties
        Assert.assertEquals(properties.remove("foo"), false)
        Assert.assertEquals(properties.remove("bar"), 1)
        Assert.assertEquals(properties.remove("baz"), 1.5)
        Assert.assertEquals(properties.remove("fuzz?"), "foobar")
        Assert.assertEquals(0, properties.size.toLong())
    }

    @Test
    fun testEventStringTypeNotEnabled() {
        val kit = MockAppboyKit()
        kit.configuration = MockKitConfiguration()
        val customAttributes = HashMap<String, String?>()
        customAttributes["foo"] = "false"
        customAttributes["bar"] = "1"
        customAttributes["baz"] = "1.5"
        customAttributes["fuzz?"] = "foobar"
        val customEvent = MPEvent.Builder("testEvent", MParticle.EventType.Location)
            .customAttributes(customAttributes)
            .build()
        kit.enableTypeDetection = false
        kit.logEvent(customEvent)
        val braze = Braze
        val events = braze.events
        Assert.assertEquals(1, events.values.size.toLong())
        val event = events.values.iterator().next()
        val properties = event.properties
        Assert.assertEquals(properties.remove("foo"), "false")
        Assert.assertEquals(properties.remove("bar"), "1")
        Assert.assertEquals(properties.remove("baz"), "1.5")
        Assert.assertEquals(properties.remove("fuzz?"), "foobar")
        Assert.assertEquals(0, properties.size.toLong())
    }
}
