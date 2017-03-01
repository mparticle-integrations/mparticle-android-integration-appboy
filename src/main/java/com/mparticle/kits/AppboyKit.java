package com.mparticle.kits;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;

import com.appboy.Appboy;
import com.appboy.AppboyGcmReceiver;
import com.appboy.AppboyUser;
import com.appboy.enums.Gender;
import com.appboy.models.outgoing.AppboyProperties;
import com.appboy.push.AppboyNotificationUtils;
import com.mparticle.MPEvent;
import com.mparticle.MParticle;
import com.mparticle.MParticle.UserAttributes;
import com.mparticle.commerce.CommerceEvent;
import com.mparticle.commerce.Product;
import com.mparticle.internal.ConfigManager;
import com.mparticle.internal.Logger;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


/**
 * Embedded version of the Appboy SDK v 1.15.3
 */
public class AppboyKit extends KitIntegration implements KitIntegration.ActivityListener, KitIntegration.AttributeListener, KitIntegration.CommerceListener, KitIntegration.EventListener, KitIntegration.PushListener {

    static final String APPBOY_KEY = "apiKey";
    public static final String PUSH_ENABLED = "push_enabled";
    private static final String PREF_KEY_HAS_SYNCED_ATTRIBUTES = "appboy::has_synced_attributes";
    private static final String PREF_KEY_CURRENT_EMAIL = "appboy::current_email";
    final Handler dataFlushHandler = new Handler();
    private Runnable dataFlushRunnable;
    final private static int FLUSH_DELAY = 5000;

    @Override
    public String getName() {
        return "Appboy";
    }

    @Override
    protected List<ReportingMessage> onKitCreate(Map<String, String> settings, Context context) {
        String key = settings.get(APPBOY_KEY);
        if (KitUtils.isEmpty(key)) {
            throw new IllegalArgumentException("Appboy key is empty.");
        }
        Appboy.configure(context, key);
        dataFlushRunnable = new Runnable() {
            @Override
            public void run() {
                if (MParticle.getInstance().getAppStateManager().isBackgrounded()) {
                    Appboy.getInstance(getContext()).requestImmediateDataFlush();
                }
            }
        };
        queueDataFlush();
        return null;
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
        if (event.getInfo() == null) {
            Appboy.getInstance(getContext()).logCustomEvent(event.getEventName());
        } else {
            AppboyProperties properties = new AppboyProperties();
            for (Map.Entry<String, String> entry : event.getInfo().entrySet()) {
                properties.addProperty(entry.getKey(), entry.getValue());
            }
            Appboy.getInstance(getContext()).logCustomEvent(event.getEventName(), properties);
        }
        queueDataFlush();
        List<ReportingMessage> messages = new LinkedList<ReportingMessage>();
        messages.add(ReportingMessage.fromEvent(this, event));
        return messages;
    }

    @Override
    public List<ReportingMessage> logScreen(String screenName, Map<String, String> screenAttributes) {
        return null;
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
            for (int i = 0; i < eventList.size(); i++) {
                try {
                    logEvent(eventList.get(i));
                    messages.add(ReportingMessage.fromEvent(this, event));
                } catch (Exception e) {
                    Logger.warning("Failed to call logCustomEvent to Appboy kit: " + e.toString());
                }
            }
            queueDataFlush();
        }
        return messages;
    }

    @Override
    public void setUserAttribute(String key, String value) {
        AppboyUser user = Appboy.getInstance(getContext()).getCurrentUser();
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
        } else {
            if (key.startsWith("$")) {
                key = key.substring(1);
            }
            user.setCustomUserAttribute(key, value);
        }
        queueDataFlush();
    }

    @Override
    public void setUserAttributeList(String key, List<String> list) {
        AppboyUser user = Appboy.getInstance(getContext()).getCurrentUser();
        String[] array = list.toArray(new String[list.size()]);
        user.setCustomAttributeArray(key, array);
        queueDataFlush();
    }

    @Override
    public boolean supportsAttributeLists() {
        return true;
    }

    private void queueDataFlush() {
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
        AppboyUser user = Appboy.getInstance(getContext()).getCurrentUser();
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
        AppboyUser user = Appboy.getInstance(getContext()).getCurrentUser();
        if (MParticle.IdentityType.CustomerId.equals(identityType)) {
            if (user == null || (user.getUserId() != null && !user.getUserId().equals(identity))) {
                Appboy.getInstance(getContext()).changeUser(identity);
                queueDataFlush();
            }
        } else if (MParticle.IdentityType.Email.equals(identityType)
                && identity != null
                && !identity.equals(getKitPreferences().getString(PREF_KEY_CURRENT_EMAIL, null))) {
            user.setEmail(identity);
            queueDataFlush();
            getKitPreferences().edit().putString(PREF_KEY_CURRENT_EMAIL, identity).apply();
        }
    }

    @Override
    public void removeUserIdentity(MParticle.IdentityType identityType) {

    }

    @Override
    public List<ReportingMessage> logout() {
        return null;
    }

    private void logTransaction(CommerceEvent event, Product product) {
        AppboyProperties purchaseProperties = new AppboyProperties();
        Map<String, String> eventAttributes = new HashMap<String, String>();
        CommerceEventUtils.extractActionAttributes(event, eventAttributes);

        String currency = eventAttributes.get(CommerceEventUtils.Constants.ATT_ACTION_CURRENCY_CODE);
        if (KitUtils.isEmpty(currency)) {
            currency = CommerceEventUtils.Constants.DEFAULT_CURRENCY_CODE;
        }
        eventAttributes.remove(CommerceEventUtils.Constants.ATT_ACTION_CURRENCY_CODE);
        for (Map.Entry<String, String> entry : eventAttributes.entrySet()) {
            purchaseProperties.addProperty(entry.getKey(), entry.getValue());
        }
        Appboy.getInstance(getContext()).logPurchase(
                product.getSku(),
                currency,
                new BigDecimal(product.getUnitPrice()),
                (int) product.getQuantity(),
                purchaseProperties
        );
    }

    @Override
    public List<ReportingMessage> onActivityStopped(Activity activity) {
        Appboy.getInstance(activity).closeSession(activity);
        List<ReportingMessage> messageList = new LinkedList<ReportingMessage>();
        messageList.add(
                new ReportingMessage(this, ReportingMessage.MessageType.APP_STATE_TRANSITION, System.currentTimeMillis(), null)
        );
        return messageList;
    }

    @Override
    public List<ReportingMessage> onActivitySaveInstanceState(Activity activity, Bundle outState) {
        return null;
    }

    @Override
    public List<ReportingMessage> onActivityDestroyed(Activity activity) {
        return null;
    }

    @Override
    public List<ReportingMessage> onActivityCreated(Activity activity, Bundle savedInstanceState) {
        return null;
    }

    @Override
    public List<ReportingMessage> onActivityStarted(Activity activity) {
        Appboy.getInstance(activity).openSession(activity);
        List<ReportingMessage> messageList = new LinkedList<ReportingMessage>();
        messageList.add(
                new ReportingMessage(this, ReportingMessage.MessageType.APP_STATE_TRANSITION, System.currentTimeMillis(), null)
        );
        return messageList;
    }

    @Override
    public List<ReportingMessage> onActivityResumed(Activity activity) {
        return null;
    }

    @Override
    public List<ReportingMessage> onActivityPaused(Activity activity) {
        return null;
    }

    @Override
    public boolean willHandlePushMessage(Intent intent) {
        if (!Boolean.parseBoolean(getSettings().get(PUSH_ENABLED))) {
            return false;
        }
        return AppboyNotificationUtils.isAppboyPushMessage(intent);
    }

    @Override
    public void onPushMessageReceived(Context context, Intent pushIntent) {
        if (Boolean.parseBoolean(getSettings().get(PUSH_ENABLED))) {
            new AppboyGcmReceiver().onReceive(context, pushIntent);
        }
    }

    @Override
    public boolean onPushRegistration(String instanceId, String senderId) {
        if (Boolean.parseBoolean(getSettings().get(PUSH_ENABLED))) {
            Appboy.getInstance(getContext()).registerAppboyGcmMessages(instanceId);
            queueDataFlush();
            return true;
        } else {
            return false;
        }
    }
}
