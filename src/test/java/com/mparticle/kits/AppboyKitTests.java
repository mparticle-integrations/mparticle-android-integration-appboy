package com.mparticle.kits;


import com.appboy.Appboy;
import com.appboy.MockAppboyUser;
import com.appboy.enums.Month;
import com.appboy.models.outgoing.AppboyProperties;
import com.mparticle.MPEvent;
import com.mparticle.MParticle;
import com.mparticle.commerce.CommerceEvent;
import com.mparticle.commerce.Product;
import com.mparticle.commerce.TransactionAttributes;
import com.mparticle.identity.IdentityApi;
import com.mparticle.identity.MParticleUser;
import com.mparticle.kits.mocks.MockAppboyKit;
import com.mparticle.kits.mocks.MockContext;
import com.mparticle.kits.mocks.MockContextApplication;
import com.mparticle.kits.mocks.MockKitConfiguration;
import com.mparticle.kits.mocks.MockUser;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static com.mparticle.internal.Logger.*;
import static com.mparticle.kits.CommerceEventUtils.Constants.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class AppboyKitTests {
    Random random = new Random();

    private AppboyKit getKit() {
        return new AppboyKit();
    }

    @Before
    public void setup() {
        MParticle.setInstance(Mockito.mock(MParticle.class));
        Mockito.when(MParticle.getInstance().Identity()).thenReturn(Mockito.mock(IdentityApi.class));
        Appboy.getInstance(new MockContext()).clearPurchases();
        Appboy.getInstance(new MockContext()).clearEvents();
    }

    @Test
    public void testGetName() throws Exception {
        String name = getKit().getName();
        assertTrue(name != null && name.length() > 0);
    }

    /**
     * Kit *should* throw an exception when they're initialized with the wrong settings.
     *
     */
    @Test
    public void testOnKitCreate() throws Exception{
        Exception e = null;
        try {
            KitIntegration kit = getKit();
            Map settings = new HashMap<>();
            settings.put("fake setting", "fake");
            kit.onKitCreate(settings, new MockContextApplication());
        }catch (Exception ex) {
            e = ex;
        }
        assertNotNull(e);
    }

    @Test
    public void testClassName() throws Exception {
        KitIntegrationFactory factory = new KitIntegrationFactory();
        Map<Integer, String> integrations = factory.getKnownIntegrations();
        String className = getKit().getClass().getName();
        for (Map.Entry<Integer, String> entry : integrations.entrySet()) {
            if (entry.getValue().equals(className)) {
                return;
            }
        }
        fail(className + " not found as a known integration.");
    }

    String hostName = "aRandomHost";
    @Test
    public void testHostSetting() throws Exception {
        Map<String, String> settings = new HashMap<>();
        settings.put(AppboyKit.HOST, hostName);
        settings.put(AppboyKit.APPBOY_KEY, "key");
        MockAppboyKit kit = new MockAppboyKit();
        kit.onKitCreate(settings, new MockContextApplication());
        assertTrue(kit.calledAuthority[0].equals(hostName));
    }

    @Test
    public void testHostSettingNull() throws Exception {
        //test that the key is set when it is passed in by the settings map
        Map<String, String> missingSettings = new HashMap<>();
        missingSettings.put(AppboyKit.APPBOY_KEY, "key");
        MockAppboyKit kit = new MockAppboyKit();
        try {
        kit.onKitCreate(missingSettings, new MockContextApplication());
        } catch (Exception e) {}
        assertTrue(kit.calledAuthority[0] == null);
    }
    @Test
    public void testHostSettingEmpty() throws Exception {
        Map<String, String> nullSettings = new HashMap<>();
        nullSettings.put(AppboyKit.HOST, null);
        nullSettings.put(AppboyKit.APPBOY_KEY, "key");
        MockAppboyKit kit = new MockAppboyKit();
        try {
            kit.onKitCreate(nullSettings, new MockContextApplication());
        } catch (Exception e) {}
        assertTrue(kit.calledAuthority[0] == null);

        nullSettings = new HashMap<>();
        nullSettings.put(AppboyKit.HOST, "");
        nullSettings.put(AppboyKit.APPBOY_KEY, "key");
        kit = new MockAppboyKit();
        try {
            kit.onKitCreate(nullSettings, new MockContextApplication());
        } catch (Exception e) {}
        assertTrue(kit.calledAuthority[0] == null);
    }

    @Test
    public void testOnModify() {
        //make sure it doesn't crash if there is no email or customerId
        Exception e = null;
        try {
            new AppboyKit().onModifyCompleted(new MockUser(new HashMap<MParticle.IdentityType, String>()), null);
        }
        catch (Exception ex) {
            e = ex;
        }
        assertNull(e);
        
        for (int i = 0; i < 4; i++) {
            final String[] values = new String[2];
            String mockEmail = "mockEmail" + i;
            String mockCustomerId = "12345" + i;

            AppboyKit kit = new AppboyKit() {
                @Override
                protected void setId(String customerId) {
                    values[0] = customerId;
                }

                @Override
                protected void setEmail(String email) {
                    if (values[0] == null) {
                        fail("customerId should have been set first");
                    }
                    values[1] = email;
                }
            };

            kit.identityType = MParticle.IdentityType.CustomerId;

            Map<MParticle.IdentityType, String> map = new HashMap<>();
            map.put(MParticle.IdentityType.Email, mockEmail);
            map.put(MParticle.IdentityType.Alias, "alias");
            map.put(MParticle.IdentityType.Facebook, "facebook");
            map.put(MParticle.IdentityType.Facebook, "fb");
            map.put(MParticle.IdentityType.CustomerId, mockCustomerId);
            switch (i) {
                case 0:
                    kit.onModifyCompleted(new MockUser(map), null);
                case 1:
                    kit.onIdentifyCompleted(new MockUser(map), null);
                case 2:
                    kit.onLoginCompleted(new MockUser(map), null);
                case 3:
                    kit.onLogoutCompleted(new MockUser(map), null);
            }
            assertEquals(mockCustomerId, values[0]);
            assertEquals(mockEmail, values[1]);
        }
    }

    @Test
    public void testAgeToDob() {
        AppboyKit kit = new MockAppboyKit();
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);

        Calendar calendar = kit.getCalendarMinusYears("5");
        assertEquals(currentYear - 5, calendar.get(Calendar.YEAR));

        calendar = kit.getCalendarMinusYears(22);
        assertEquals(currentYear - 22, calendar.get(Calendar.YEAR));

//        round down doubles
        calendar = kit.getCalendarMinusYears("5.001");
        assertEquals(currentYear - 5, calendar.get(Calendar.YEAR));

        calendar = kit.getCalendarMinusYears("5.9");
        assertEquals(currentYear - 5, calendar.get(Calendar.YEAR));

        //invalid ages (negative, non numeric), don't get set
        assertNull(kit.getCalendarMinusYears("asdv"));
        assertNull(kit.getCalendarMinusYears(-1));
    }

    @Test
    public void testSetUserAttributeAge() {
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        AppboyKit kit = new MockAppboyKit();
        MockAppboyUser currentUser = (MockAppboyUser)Appboy.getInstance(null).getCurrentUser();

        assertEquals(-1, currentUser.dobDay);
        assertEquals(-1, currentUser.dobYear);
        assertNull(currentUser.dobMonth);

        kit.setUserAttribute(MParticle.UserAttributes.AGE, "100");
        assertEquals(currentYear - 100, currentUser.dobYear);
        assertEquals(1, currentUser.dobDay);
        assertEquals(Month.JANUARY, currentUser.dobMonth);
    }

    @Test
    public void testSetUserDoB() {
        AppboyKit kit = new MockAppboyKit();
        MockAppboyUser currentUser = (MockAppboyUser)Appboy.getInstance(null).getCurrentUser();

        final String[] errorMessage = new String[1];
        setLogHandler(new DefaultLogHandler() {
            @Override
            public void log(MParticle.LogLevel priority, Throwable error, String messages) {
                if (priority == MParticle.LogLevel.ERROR) {
                    errorMessage[0] = messages;
                }
            }
        });

        //valid
        kit.setUserAttribute("dob", "1999-11-05");
        assertEquals(1999, currentUser.dobYear);
        assertEquals(05, currentUser.dobDay);
        assertEquals(Month.NOVEMBER, currentUser.dobMonth);
        assertNull(errorMessage[0]);

        //future
        kit.setUserAttribute("dob", "2999-2-15");
        assertEquals(2999, currentUser.dobYear);
        assertEquals(15, currentUser.dobDay);
        assertEquals(Month.FEBRUARY, currentUser.dobMonth);
        assertNull(errorMessage[0]);


        //bad formate (shouldn't crash, but should message)
        Exception ex = null;
        try {
            kit.setUserAttribute("dob", "2kjb.21h045");
            assertEquals(2999, currentUser.dobYear);
            assertEquals(15, currentUser.dobDay);
            assertEquals(Month.FEBRUARY, currentUser.dobMonth);
            assertNotNull(errorMessage[0]);
        } catch (Exception e) {
            ex = e;
        }
        assertNull(ex);
    }

    @Test
    public void setIdentityType() {
        String[] possibleValues = new String[]{"Other","CustomerId", "Facebook",
                "Twitter", "Google", "Microsoft",
                "Yahoo", "Email", "Alias"};
        String mpid = "MPID";

        for (String val: possibleValues) {
            AppboyKit kit = getKit();
            Map<String, String> settings = new HashMap<>();
            settings.put(AppboyKit.USER_IDENTIFICATION_TYPE, val);
            kit.setIdentityType(settings);
            assertNotNull(kit.identityType);
            assertEquals(val.toLowerCase(), kit.identityType.name().toLowerCase());
            assertFalse(kit.isMpidIdentityType);
        }

        Map<String, String> settings = new HashMap<>();
        settings.put(AppboyKit.USER_IDENTIFICATION_TYPE, mpid);
        AppboyKit kit = getKit();
        kit.setIdentityType(settings);
        assertNull(kit.identityType);
        assertTrue(kit.isMpidIdentityType);

    }

    @Test
    public void setId() {
        Map<MParticle.IdentityType, String> userIdentities = new HashMap<>();
        MParticleUser user = Mockito.mock(MParticleUser.class);
        Mockito.when(user.getUserIdentities()).thenReturn(userIdentities);
        Long mockId = random.nextLong();
        Mockito.when(user.getId()).thenReturn(mockId);

        assertEquals(String.valueOf(mockId), getKit().getIdentity(true, null, user));

        for (MParticle.IdentityType identityType: MParticle.IdentityType.values()) {
            String identityValue = String.valueOf(random.nextLong());
            userIdentities.put(identityType, identityValue);
            assertEquals(identityValue, getKit().getIdentity(false, identityType, user));
        }

        assertNull(getKit().getIdentity(false, null, null));
    }

    @Test
    public void addRemoveAttributeFromEventTest() {
        AppboyKit kit = new MockAppboyKit();
        MockAppboyUser currentUser = (MockAppboyUser) Appboy.getInstance(null).getCurrentUser();

        kit.setConfiguration(new MockKitConfiguration() {
            @Override
            public Map<Integer, String> getEventAttributesAddToUser() {
                Map<Integer, String> map = new HashMap();
                map.put(KitUtils.hashForFiltering(MParticle.EventType.Navigation + "Navigation Event" + "key1"), "output");
                return map;
            }

            @Override
            public Map<Integer, String> getEventAttributesRemoveFromUser() {
                Map<Integer, String> map = new HashMap();
                map.put(KitUtils.hashForFiltering(MParticle.EventType.Location + "location event" + "key1"), "output");
                return map;
            }
        });

        Map<String, String> customAttributes = new HashMap<>();
        customAttributes.put("key1", "value1");

        kit.logEvent(new MPEvent.Builder("Navigation Event", MParticle.EventType.Navigation)
                .customAttributes(customAttributes)
                .build());

        List<String> attributes = currentUser.getCustomAttributeArray().get("output");
        assertEquals(1, attributes.size());
        assertEquals("value1", attributes.get(0));


        kit.logEvent(new MPEvent.Builder("location event", MParticle.EventType.Location)
                .customAttributes(customAttributes)
                .build());

        attributes = currentUser.getCustomAttributeArray().get("output");
        assertEquals(0, attributes.size());
    }

    @Test
    public void testPurchaseCurrency() {
        AppboyKit kit = new MockAppboyKit();
        Product product = new Product.Builder("product name","sku1",4.5)
                .build();
        CommerceEvent commerceEvent = new CommerceEvent.Builder(Product.CHECKOUT, product)
                .currency("Moon Dollars")
                .build();

        kit.logTransaction(commerceEvent, product);

        Appboy appboy = Appboy.getInstance(new MockContext());
        List<AppboyPurchase> purchases = appboy.getPurchases();

        assertEquals(1, purchases.size());
        AppboyPurchase purchase = purchases.get(0);

        assertEquals("Moon Dollars", purchase.getCurrency());
        assertNull(purchase.getPurchaseProperties().getProperties().get(ATT_ACTION_CURRENCY_CODE));
    }

    @Test
    public void testPurchaseDefaultCurrency() {
        AppboyKit kit = new MockAppboyKit();
        Product product = new Product.Builder("product name","sku1",4.5)
                .build();
        CommerceEvent commerceEvent = new CommerceEvent.Builder(Product.CHECKOUT, product)
                .build();

        kit.logTransaction(commerceEvent, product);

        Appboy appboy = Appboy.getInstance(new MockContext());
        List<AppboyPurchase> purchases = appboy.getPurchases();

        assertEquals(1, purchases.size());
        AppboyPurchase purchase = purchases.get(0);

        assertEquals(CommerceEventUtils.Constants.DEFAULT_CURRENCY_CODE, purchase.getCurrency());
        assertNull(purchase.getPurchaseProperties().getProperties().get(ATT_ACTION_CURRENCY_CODE));
    }

    @Test
    public void testPurchase() {
        AppboyKit kit = new MockAppboyKit();
        Map<String, String> customAttributes = new HashMap<>();
        customAttributes.put("key1", "value1");
        customAttributes.put("key #2", "value #3");
        TransactionAttributes transactionAttributes = new TransactionAttributes("the id")
                .setTax(100.0)
                .setShipping(12.0)
                .setRevenue(99.0)
                .setCouponCode("coupon code")
                .setAffiliation("the affiliation");
        Product product = new Product.Builder("product name","sku1",4.5)
                .quantity(5.0)
                .build();
        CommerceEvent commerceEvent = new CommerceEvent.Builder(Product.CHECKOUT, product)
                .currency("Moon Dollars")
                .productListName("product list name")
                .productListSource("the source")
                .customAttributes(customAttributes)
                .transactionAttributes(transactionAttributes)
                .build();

        kit.logTransaction(commerceEvent, product);

        Appboy appboy = Appboy.getInstance(new MockContext());
        List<AppboyPurchase> purchases = appboy.getPurchases();

        assertEquals(1, purchases.size());
        AppboyPurchase purchase = purchases.get(0);

        assertEquals("Moon Dollars", purchase.getCurrency());
        assertEquals(5.0, purchase.getQuantity(), 0.01);
        assertEquals("sku1", purchase.getSku());
        assertEquals(new BigDecimal(4.5), purchase.getUnitPrice());
        assertNotNull(purchase.getPurchaseProperties());

        Map<String, Object> properties = purchase.getPurchaseProperties().getProperties();
        assertEquals(properties.remove(ATT_SHIPPING), 12.0);
        assertEquals(properties.remove(ATT_ACTION_PRODUCT_LIST_SOURCE), "the source");
        assertEquals(properties.remove(ATT_TAX), 100.0);
        assertEquals(properties.remove(ATT_TOTAL), 99.0);
        assertEquals(properties.remove(ATT_ACTION_PRODUCT_ACTION_LIST), "product list name");
        assertEquals(properties.remove(ATT_PRODUCT_COUPON_CODE), "coupon code");
        assertEquals(properties.remove(ATT_TRANSACTION_ID), "the id");
        assertEquals(properties.remove(ATT_AFFILIATION), "the affiliation");

        assertEquals(0, properties.size());
    }

    @Test
    public void setUserAttributeTyped() {
        AppboyKit kit = new MockAppboyKit();
        kit.enableTypeDetection = true;
        MockAppboyUser currentUser = (MockAppboyUser)Appboy.getInstance(null).getCurrentUser();

        kit.setUserAttribute("foo", "true");
        assertTrue(currentUser.getCustomUserAttributes().get("foo") instanceof Boolean);
        assertEquals(currentUser.getCustomUserAttributes().get("foo"), true);

        kit.setUserAttribute("foo", "1");
        assertTrue(currentUser.getCustomUserAttributes().get("foo") instanceof Integer);
        assertEquals(currentUser.getCustomUserAttributes().get("foo"), 1);

        kit.setUserAttribute("foo", "1.1");
        assertTrue(currentUser.getCustomUserAttributes().get("foo") instanceof Double);
        assertEquals(currentUser.getCustomUserAttributes().get("foo"), 1.1);

        kit.setUserAttribute("foo", "bar");
        assertTrue(currentUser.getCustomUserAttributes().get("foo") instanceof String);
        assertEquals(currentUser.getCustomUserAttributes().get("foo"), "bar");
    }

    @Test
    public void testEventStringType() {
        AppboyKit kit = new MockAppboyKit();
        kit.setConfiguration(new MockKitConfiguration());

        Map<String, String> customAttributes = new HashMap<>();
        customAttributes.put("foo", "false");
        customAttributes.put("bar", "1");
        customAttributes.put("baz", "1.5");
        customAttributes.put("fuzz?", "foobar");

        MPEvent customEvent = new MPEvent.Builder("testEvent", MParticle.EventType.Location)
                .customAttributes(customAttributes)
                .build();

        kit.enableTypeDetection = true;
        kit.logEvent(customEvent);

        Appboy appboy = Appboy.getInstance(new MockContext());
        Map<String, AppboyProperties> events = appboy.getEvents();

        assertEquals(1, events.values().size());
        AppboyProperties event = events.values().iterator().next();

        Map<String, Object> properties = event.getProperties();
        assertEquals(properties.remove("foo"), false);
        assertEquals(properties.remove("bar"), 1);
        assertEquals(properties.remove("baz"), 1.5);
        assertEquals(properties.remove("fuzz?"), "foobar");

        assertEquals(0, properties.size());
    }

    @Test
    public void testEventStringTypeNotEnabled() {
        AppboyKit kit = new MockAppboyKit();
        kit.setConfiguration(new MockKitConfiguration());

        Map<String, String> customAttributes = new HashMap<>();
        customAttributes.put("foo", "false");
        customAttributes.put("bar", "1");
        customAttributes.put("baz", "1.5");
        customAttributes.put("fuzz?", "foobar");

        MPEvent customEvent = new MPEvent.Builder("testEvent", MParticle.EventType.Location)
                .customAttributes(customAttributes)
                .build();

        kit.enableTypeDetection = false;
        kit.logEvent(customEvent);

        Appboy appboy = Appboy.getInstance(new MockContext());
        Map<String, AppboyProperties> events = appboy.getEvents();

        assertEquals(1, events.values().size());
        AppboyProperties event = events.values().iterator().next();

        Map<String, Object> properties = event.getProperties();
        assertEquals(properties.remove("foo"), "false");
        assertEquals(properties.remove("bar"), "1");
        assertEquals(properties.remove("baz"), "1.5");
        assertEquals(properties.remove("fuzz?"), "foobar");

        assertEquals(0, properties.size());
    }
}