package com.mparticle.kits;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.appboy.IBrazeEndpointProvider;
import com.appboy.enums.Gender;
import com.appboy.enums.Month;
import com.appboy.enums.SdkFlavor;
import com.braze.Braze;
import com.braze.BrazeActivityLifecycleCallbackListener;
import com.braze.BrazeUser;
import com.braze.configuration.BrazeConfig;
import com.braze.enums.BrazeSdkMetadata;
import com.braze.models.outgoing.BrazeProperties;
import com.braze.push.BrazeFirebaseMessagingService;
import com.braze.push.BrazeNotificationUtils;
import com.google.firebase.messaging.RemoteMessage;
import com.mparticle.MPEvent;
import com.mparticle.MParticle;
import com.mparticle.MParticle.UserAttributes;
import com.mparticle.commerce.CommerceEvent;
import com.mparticle.commerce.Product;
import com.mparticle.identity.MParticleUser;
import com.mparticle.internal.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


/**
 * mParticle client-side Appboy integration
 */
public class AppboyKit extends KitIntegration implements KitIntegration.AttributeListener, KitIntegration.CommerceListener, KitIntegration.EventListener, KitIntegration.PushListener, KitIntegration.IdentityListener {

    static final String APPBOY_KEY = "apiKey";
    static final String FORWARD_SCREEN_VIEWS = "forwardScreenViews";
    static final String EXPAND_NON_PURCHASE_COMMERCE_EVENTS = "expandNonPurchaseCommerceEvents";
    static final String USER_IDENTIFICATION_TYPE = "userIdentificationType";
    static final String ENABLE_TYPE_DETECTION = "enableTypeDetection";

    static final String HOST = "host";
    boolean isMpidIdentityType = false;
    MParticle.IdentityType identityType;

    public static final String PUSH_ENABLED = "push_enabled";
    private static final String PREF_KEY_HAS_SYNCED_ATTRIBUTES = "appboy::has_synced_attributes";
    private static final String PREF_KEY_CURRENT_EMAIL = "appboy::current_email";
    final Handler dataFlushHandler = new Handler();
    private Runnable dataFlushRunnable;
    final private static int FLUSH_DELAY = 5000;
    private boolean forwardScreenViews = false;
    private boolean expandNonPurchaseCommerceEvents = true;
    boolean enableTypeDetection = false;

    public static boolean setDefaultAppboyLifecycleCallbackListener = true;

    @Override
    public String getName() {
        return "Appboy";
    }

    @Override
    protected List<ReportingMessage> onKitCreate(Map<String, String> settings, Context context) {
        String key = settings.get(APPBOY_KEY);
        if (KitUtils.isEmpty(key)) {
            throw new IllegalArgumentException("Braze key is empty.");
        }

        //try to get endpoint from the host setting
        String authority = settings.get(HOST);
        if (!KitUtils.isEmpty(authority)) {
            setAuthority(authority);
        }

        String enableDetectionType = settings.get(ENABLE_TYPE_DETECTION);
        if (!KitUtils.isEmpty(enableDetectionType)) {
            try {
                this.enableTypeDetection = Boolean.parseBoolean(enableDetectionType);
            } catch (Exception e) {
                Logger.warning("Braze, unable to parse \"enableDetectionType\"");
            }
        }

        forwardScreenViews = Boolean.parseBoolean(settings.get(FORWARD_SCREEN_VIEWS));
        expandNonPurchaseCommerceEvents = Boolean.parseBoolean(settings.get(EXPAND_NON_PURCHASE_COMMERCE_EVENTS));
        BrazeConfig config = new BrazeConfig.Builder().setApiKey(key)
            .setSdkFlavor(SdkFlavor.MPARTICLE)
            .setSdkMetadata(EnumSet.of(BrazeSdkMetadata.MPARTICLE))
            .build();
        Braze.configure(context, config);
        dataFlushRunnable = new Runnable() {
            @Override
            public void run() {
                if (getKitManager().isBackgrounded()) {
                   Braze.getInstance(getContext()).requestImmediateDataFlush();
                }
            }
        };
        queueDataFlush();
        if (setDefaultAppboyLifecycleCallbackListener) {
            ((Application) context.getApplicationContext()).registerActivityLifecycleCallbacks(new BrazeActivityLifecycleCallbackListener());
        }
        setIdentityType(settings);
        return null;
    }

    void setIdentityType(Map<String, String> settings) {
        String userIdentificationType = settings.get(USER_IDENTIFICATION_TYPE);
        if (!KitUtils.isEmpty(userIdentificationType)) {
            if (userIdentificationType.equals("MPID")) {
                isMpidIdentityType = true;
            } else {
                identityType = MParticle.IdentityType.valueOf(userIdentificationType);
            }
        }
    }

    @Override
    public List<ReportingMessage> setOptOut(boolean optedOut) {
        return null;
    }

    @Override
    public List<ReportingMessage> leaveBreadcrumb(String breadcrumb) {
        return null;
    }

    @Override
    public List<ReportingMessage> logError(String message, Map<String, String> errorAttributes) {
        return null;
    }

    @Override
    public List<ReportingMessage> logException(Exception exception, Map<String, String> exceptionAttributes, String message) {
        return null;
    }

    @Override
    public List<ReportingMessage> logEvent(MPEvent event) {
        Map<String, Object> newAttributes = new HashMap<>();
        if (event.getCustomAttributes() == null) {
           Braze.getInstance(getContext()).logCustomEvent(event.getEventName());
        } else {
            BrazeProperties properties = new BrazeProperties();
            BrazeUser user = Braze.getInstance(getContext()).getCurrentUser();
            BrazePropertiesSetter brazePropertiesSetter = new BrazePropertiesSetter(properties, enableTypeDetection);
            UserAttributeSetter userAttributeSetter = new UserAttributeSetter(user, enableTypeDetection);
            for (Map.Entry<String, String> entry : event.getCustomAttributeStrings().entrySet()) {
                newAttributes.put(entry.getKey(), brazePropertiesSetter.parseValue(entry.getKey(), entry.getValue()));
                Integer hashedKey = KitUtils.hashForFiltering(event.getEventType().toString() + event.getEventName() + entry.getKey());
                Map<Integer, String> attributeMap = getConfiguration().getEventAttributesAddToUser();
                if (attributeMap.containsKey(hashedKey)) {
                    user.addToCustomAttributeArray(attributeMap.get(hashedKey), entry.getValue());
                }
                attributeMap = getConfiguration().getEventAttributesRemoveFromUser();
                if (attributeMap.containsKey(hashedKey)) {
                    user.removeFromCustomAttributeArray(attributeMap.get(hashedKey), entry.getValue());
                }
                attributeMap = getConfiguration().getEventAttributesSingleItemUser();
                if (attributeMap.containsKey(hashedKey)) {
                    userAttributeSetter.parseValue(attributeMap.get(hashedKey), entry.getValue());
                }
            }
            Braze.getInstance(getContext()).logCustomEvent(event.getEventName(), properties);
        }
        queueDataFlush();
        return Collections.singletonList(ReportingMessage.fromEvent(this, event).setAttributes(newAttributes));
    }

    @Override
    public List<ReportingMessage> logScreen(String screenName, Map<String, String> screenAttributes) {
        if (forwardScreenViews) {
            if (screenAttributes == null) {
                Braze.getInstance(getContext()).logCustomEvent(screenName);
            } else {
                BrazeProperties properties = new BrazeProperties();
                StringTypeParser propertyParser = new BrazePropertiesSetter(properties, enableTypeDetection);
                for (Map.Entry<String, String> entry : screenAttributes.entrySet()) {
                    propertyParser.parseValue(entry.getKey(), entry.getValue());
                }
                Braze.getInstance(getContext()).logCustomEvent(screenName, properties);
            }
            queueDataFlush();
            List<ReportingMessage> messages = new LinkedList<ReportingMessage>();
            messages.add(new ReportingMessage(this, ReportingMessage.MessageType.SCREEN_VIEW, System.currentTimeMillis(), screenAttributes));
            return messages;
        } else {
            return null;
        }
    }


    @Override
    public List<ReportingMessage> logLtvIncrease(BigDecimal valueIncreased, BigDecimal valueTotal, String eventName, Map<String, String> contextInfo) {
        return null;
    }

    @Override
    public List<ReportingMessage> logEvent(CommerceEvent event) {
        List<ReportingMessage> messages = new LinkedList<ReportingMessage>();
        if (!KitUtils.isEmpty(event.getProductAction()) &&
                event.getProductAction().equalsIgnoreCase(Product.PURCHASE) &&
                event.getProducts().size() > 0) {
            List<Product> productList = event.getProducts();
            for (Product product : productList) {
                logTransaction(event, product);
            }
            messages.add(ReportingMessage.fromEvent(this, event));
            queueDataFlush();
            return messages;
        }
        List<MPEvent> eventList = CommerceEventUtils.expand(event);
        if (eventList != null) {
            if (expandNonPurchaseCommerceEvents) {
                for (int i = 0; i < eventList.size(); i++) {
                    try {
                        logEvent(eventList.get(i));
                        messages.add(ReportingMessage.fromEvent(this, event));
                    } catch (Exception e) {
                        Logger.warning("Failed to call logCustomEvent to Braze kit: " + e.toString());
                    }
                }
            } else {
                JSONArray productArray = new JSONArray();
                for (int i = 0; i < eventList.size(); i++) {
                    Map<String, Object> newAttributes = eventList.get(i).getCustomAttributes();
                    newAttributes.put("custom attributes", event.getCustomAttributes());
                    productArray.put(newAttributes);
                }
                try {
                    JSONObject json = new JSONObject().put("products", productArray);
                    TransactionAttributes transactionAttributes = event.getTransactionAttributes();
                    if (transactionAttributes != null) {
                        json.put("Transaction ID",  transactionAttributes.getId();
                    }
                    BrazeProperties brazeProperties = new BrazeProperties(json);
                    Braze.getInstance(getContext()).logCustomEvent(eventList.get(0).getEventName(), brazeProperties);
                    messages.add(ReportingMessage.fromEvent(this, event));
                } catch (JSONException e) {
                    Logger.warning("Failed to call logCustomEvent to Braze kit: " + e.toString());
                }
            }
            queueDataFlush();
        }
        return messages;
    }

    @Override
    public void setUserAttribute(String key, String value) {
        BrazeUser user = Braze.getInstance(getContext()).getCurrentUser();
        UserAttributeSetter userAttributeSetter = new UserAttributeSetter(user, enableTypeDetection);
        if (UserAttributes.CITY.equals(key)) {
            user.setHomeCity(value);
        } else if (UserAttributes.COUNTRY.equals(key)) {
            user.setCountry(value);
        } else if (UserAttributes.FIRSTNAME.equals(key)) {
            user.setFirstName(value);
        } else if (UserAttributes.GENDER.equals(key)) {
            if (value.contains("fe")) {
                user.setGender(Gender.FEMALE);
            } else {
                user.setGender(Gender.MALE);
            }
        } else if (UserAttributes.LASTNAME.equals(key)) {
            user.setLastName(value);
        } else if (UserAttributes.MOBILE_NUMBER.equals(key)) {
            user.setPhoneNumber(value);
        } else if (UserAttributes.AGE.equals(key)) {
            Calendar calendar = getCalendarMinusYears(value);
            if (calendar != null) {
                user.setDateOfBirth(calendar.get(Calendar.YEAR), Month.JANUARY, 1);
            } else {
                Logger.error("unable to set DateOfBirth for " + UserAttributes.AGE + " = " + value);
            }
        } else if ("dob".equals(key)) {
            // Expected Date Format @"yyyy'-'MM'-'dd"
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            try {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(dateFormat.parse(value));
                int year = calendar.get(Calendar.YEAR);
                int month = calendar.get(Calendar.MONTH);
                Month monthEnumValue = Month.getMonth(month);
                int day = calendar.get(Calendar.DAY_OF_MONTH);
                user.setDateOfBirth(year, monthEnumValue, day);
            } catch (Exception e) {
                Logger.error("unable to set DateOfBirth for \"dob\" = " + value + ". Exception: " + e.getMessage());
            }
        } else if (UserAttributes.ZIPCODE.equals(key)) {
            user.setCustomUserAttribute("Zip", value);
        } else {
            if (key.startsWith("$")) {
                key = key.substring(1);
            }
            userAttributeSetter.parseValue(key, value);
        }
        queueDataFlush();
    }

    @Override
    public void setUserAttributeList(String key, List<String> list) {
        BrazeUser user = Braze.getInstance(getContext()).getCurrentUser();
        String[] array = list.toArray(new String[list.size()]);
        user.setCustomAttributeArray(key, array);
        queueDataFlush();
    }

    @Override
    public boolean supportsAttributeLists() {
        return true;
    }

    protected void queueDataFlush() {
        dataFlushHandler.removeCallbacks(dataFlushRunnable);
        dataFlushHandler.postDelayed(dataFlushRunnable, FLUSH_DELAY);
    }

    /**
     * This is called when the Kit is added to the mParticle SDK, typically on app-startup.
     */
    @Override
    public void setAllUserAttributes(Map<String, String> attributes, Map<String, List<String>> attributeLists) {
        if (!getKitPreferences().getBoolean(PREF_KEY_HAS_SYNCED_ATTRIBUTES, false)) {
            for (Map.Entry<String, String> entry : attributes.entrySet()) {
                setUserAttribute(entry.getKey(), entry.getValue());
            }
            for (Map.Entry<String, List<String>> entry : attributeLists.entrySet()) {
                setUserAttributeList(entry.getKey(), entry.getValue());
            }
            getKitPreferences().edit().putBoolean(PREF_KEY_HAS_SYNCED_ATTRIBUTES, true).apply();
        }
    }

    @Override
    public void removeUserAttribute(String key) {
        BrazeUser user = Braze.getInstance(getContext()).getCurrentUser();
        if (UserAttributes.CITY.equals(key)) {
            user.setHomeCity(null);
        } else if (UserAttributes.COUNTRY.equals(key)) {
            user.setCountry(null);
        } else if (UserAttributes.FIRSTNAME.equals(key)) {
            user.setFirstName(null);
        } else if (UserAttributes.GENDER.equals(key)) {
            user.setGender(null);
        } else if (UserAttributes.LASTNAME.equals(key)) {
            user.setLastName(null);
        } else if (UserAttributes.MOBILE_NUMBER.equals(key)) {
            user.setPhoneNumber(null);
        } else {
            if (key.startsWith("$")) {
                key = key.substring(1);
            }
            user.unsetCustomUserAttribute(key);
        }
        queueDataFlush();
    }

    @Override
    public void setUserIdentity(MParticle.IdentityType identityType, String identity) {

    }

    @Override
    public void removeUserIdentity(MParticle.IdentityType identityType) {

    }

    @Override
    public List<ReportingMessage> logout() {
        return null;
    }

    void logTransaction(CommerceEvent event, Product product) {
        final BrazeProperties purchaseProperties = new BrazeProperties();
        final String[] currency = new String[1];
        final StringTypeParser commerceTypeParser = new BrazePropertiesSetter(purchaseProperties, enableTypeDetection);
        CommerceEventUtils.OnAttributeExtracted onAttributeExtracted = new CommerceEventUtils.OnAttributeExtracted() {

            @Override
            public void onAttributeExtracted(String key, String value) {
                if (!checkCurrency(key, value)) {
                    commerceTypeParser.parseValue(key, value);
                }
            }

            @Override
            public void onAttributeExtracted(String key, double value) {
                if (!checkCurrency(key, value)) {
                    purchaseProperties.addProperty(key, value);
                }
            }

            @Override
            public void onAttributeExtracted(String key, int value) {
                purchaseProperties.addProperty(key, value);
            }

            @Override
            public void onAttributeExtracted(Map<String, String> attributes) {
                for (Map.Entry<String, String> entry: attributes.entrySet()) {
                    if (!checkCurrency(entry.getKey(), entry.getValue())) {
                        commerceTypeParser.parseValue(entry.getKey(), entry.getValue());
                    }
                }
            }

            private boolean checkCurrency(String key, Object value) {
                if (CommerceEventUtils.Constants.ATT_ACTION_CURRENCY_CODE.equals(key)) {
                    currency[0] = value != null ? value.toString(): null;
                    return true;
                } else {
                    return false;
                }
            }
        };
        CommerceEventUtils.extractActionAttributes(event, onAttributeExtracted);
        purchaseProperties.addProperty("custom_attributes", event.getCustomAttributes());

        String currencyValue = currency[0];
        if (KitUtils.isEmpty(currencyValue)) {
            currencyValue = CommerceEventUtils.Constants.DEFAULT_CURRENCY_CODE;
        }
        Braze.getInstance(getContext()).logPurchase(
                product.getSku(),
                currencyValue,
                new BigDecimal(product.getUnitPrice()),
                (int) product.getQuantity(),
                purchaseProperties
        );
    }

    @Override
    public boolean willHandlePushMessage(Intent intent) {
        if (!Boolean.parseBoolean(getSettings().get(PUSH_ENABLED))) {
            return false;
        }
        return BrazeNotificationUtils.isAppboyPushMessage(intent);
    }

    @Override
    public void onPushMessageReceived(Context context, Intent pushIntent) {
        if (Boolean.parseBoolean(getSettings().get(PUSH_ENABLED))) {
            BrazeFirebaseMessagingService.handleBrazeRemoteMessage(context, new RemoteMessage(pushIntent.getExtras()));
        }
    }

    @Override
    public boolean onPushRegistration(String instanceId, String senderId) {
        if (Boolean.parseBoolean(getSettings().get(PUSH_ENABLED))) {
            Braze.getInstance(getContext()).registerAppboyPushMessages(instanceId);
            queueDataFlush();
            return true;
        } else {
            return false;
        }
    }

    protected void setAuthority(final String authority) {
        Braze.setEndpointProvider(
                new IBrazeEndpointProvider() {
                    @Override
                    public Uri getApiEndpoint(Uri appboyEndpoint) {
                        return appboyEndpoint.buildUpon()
                                .authority(authority).build();
                    }
                }
        );
    }

    @Override
    public void onIdentifyCompleted(MParticleUser mParticleUser, FilteredIdentityApiRequest filteredIdentityApiRequest) {
        updateUser(mParticleUser);
    }

    @Override
    public void onLoginCompleted(MParticleUser mParticleUser, FilteredIdentityApiRequest filteredIdentityApiRequest) {
        updateUser(mParticleUser);
    }

    @Override
    public void onLogoutCompleted(MParticleUser mParticleUser, FilteredIdentityApiRequest filteredIdentityApiRequest) {
        updateUser(mParticleUser);
    }

    @Override
    public void onModifyCompleted(MParticleUser mParticleUser, FilteredIdentityApiRequest filteredIdentityApiRequest) {
        updateUser(mParticleUser);
    }

    private void updateUser(@NonNull MParticleUser mParticleUser) {
        String identity = getIdentity(isMpidIdentityType, identityType, mParticleUser);
        String email = mParticleUser.getUserIdentities().get(MParticle.IdentityType.Email);
        if (identity != null) {
            setId(identity);
        }
        if (email != null) {
            setEmail(email);
        }
    }

    String getIdentity(boolean isMpidIdentityType, @Nullable MParticle.IdentityType identityType, @NonNull MParticleUser mParticleUser) {
        String identity = null;
        if (isMpidIdentityType) {
            identity = String.valueOf(mParticleUser.getId());
        } else if (identityType != null) {
            identity = mParticleUser.getUserIdentities().get(identityType);
        }
        return identity;
    }

    protected void setId(String customerId) {
        BrazeUser user = Braze.getInstance(getContext()).getCurrentUser();
        if (user == null || (user.getUserId() != null && !user.getUserId().equals(customerId))) {
            Braze.getInstance(getContext()).changeUser(customerId);
            queueDataFlush();
        }
    }

    protected void setEmail(String email) {
        if (!email.equals(getKitPreferences().getString(PREF_KEY_CURRENT_EMAIL, null))) {
            BrazeUser user = Braze.getInstance(getContext()).getCurrentUser();
            user.setEmail(email);
            queueDataFlush();
            getKitPreferences().edit().putString(PREF_KEY_CURRENT_EMAIL, email).apply();
        }
    }

    @Override
    public void onUserIdentified(MParticleUser mParticleUser) {
        
    }

    void addToProperties(BrazeProperties properties, String key, String value) {
        try {
            if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
                properties.addProperty(key, Boolean.parseBoolean(value));
            } else {
                double doubleValue = Double.parseDouble(value);
                if ((doubleValue % 1) == 0) {
                    properties.addProperty(key, Integer.parseInt(value));
                } else {
                    properties.addProperty(key, doubleValue);
                }
            }
        } catch (Exception e) {
            properties.addProperty(key, value);
        }
    }

    @Nullable
    Calendar getCalendarMinusYears(String yearsString) {
        try {
            int years = Integer.parseInt(yearsString);
            return getCalendarMinusYears(years);
        } catch (NumberFormatException ignored) {
            try {
                double years = Double.parseDouble(yearsString);
                return getCalendarMinusYears((int)years);
            } catch (NumberFormatException ignoredToo) {

            }
        }
        return null;
    }

    @Nullable
    Calendar getCalendarMinusYears(int years) {
        if (years >= 0) {
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) - years);
            return calendar;
        } else {
            return null;
        }
    }

    abstract static class StringTypeParser {
        boolean enableTypeDetection;

        StringTypeParser(boolean enableTypeDetection) {
            this.enableTypeDetection = enableTypeDetection;
        }

        Object parseValue(String key, String value) {
            if (!enableTypeDetection) {
                toString(key, value);
                return value;
            }
            if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
                boolean newBool = Boolean.parseBoolean(value);
                toBoolean(key, newBool);
                return newBool;
            } else {
                try {
                    if (value.contains(".")) {
                        double doubleValue = Double.parseDouble(value);
                        toDouble(key, doubleValue);
                        return doubleValue;
                    } else {
                        long newLong = Long.parseLong(value);
                        if (newLong <= Integer.MAX_VALUE && newLong >= Integer.MIN_VALUE) {
                            int newInt = (int) newLong;
                            toInt(key, newInt);
                            return newInt;
                        } else {
                            toLong(key, newLong);
                            return newLong;
                        }
                    }
                } catch (NumberFormatException nfe) {
                    toString(key, value);
                    return value;
                }
            }
        }
        abstract void toInt(String key, int value);
        abstract void toLong(String key, long value);
        abstract void toDouble(String key, double value);
        abstract void toBoolean(String key, boolean value);
        abstract void toString(String key, String value);
    }

    class BrazePropertiesSetter extends StringTypeParser {
        BrazeProperties properties;

        BrazePropertiesSetter(BrazeProperties properties, boolean enableTypeDetection) {
            super(enableTypeDetection);
            this.properties = properties;
        }

        @Override
        void toInt(String key, int value) {
            properties.addProperty(key, value);
        }

        @Override
        void toLong(String key, long value) {
            properties.addProperty(key, value);
        }

        @Override
        void toDouble(String key, double value) {
            properties.addProperty(key, value);
        }

        @Override
        void toBoolean(String key, boolean value) {
            properties.addProperty(key, value);
        }

        @Override
        void toString(String key, String value) {
            properties.addProperty(key, value);
        }
    }

    class UserAttributeSetter extends StringTypeParser {
        BrazeUser brazeUser;

        UserAttributeSetter(BrazeUser brazeUser, boolean enableTypeDetection) {
            super(enableTypeDetection);
            this.brazeUser = brazeUser;
        }

        @Override
        void toInt(String key, int value) {
            brazeUser.setCustomUserAttribute(key, value);
        }

        @Override
        void toLong(String key, long value) {
            brazeUser.setCustomUserAttribute(key, value);
        }

        @Override
        void toDouble(String key, double value) {
            brazeUser.setCustomUserAttribute(key, value);
        }

        @Override
        void toBoolean(String key, boolean value) {
            brazeUser.setCustomUserAttribute(key, value);
        }

        @Override
        void toString(String key, String value) {
            brazeUser.setCustomUserAttribute(key, value);
        }
    }
}
