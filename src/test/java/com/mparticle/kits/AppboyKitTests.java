package com.mparticle.kits;


import android.app.Activity;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.telephony.TelephonyManager;
import android.util.SparseBooleanArray;
import android.view.Display;

import com.appboy.Appboy;
import com.appboy.AppboyUser;
import com.appboy.MockAppboyUser;
import com.appboy.enums.Month;
import com.mparticle.MParticle;
import com.mparticle.UserAttributeListener;
import com.mparticle.commerce.Cart;
import com.mparticle.consent.ConsentState;
import com.mparticle.identity.IdentityApi;
import com.mparticle.identity.MParticleUser;
import com.mparticle.internal.BackgroundTaskHandler;
import com.mparticle.internal.CoreCallbacks;
import com.mparticle.internal.Logger;
import com.mparticle.internal.ReportingManager;

import junit.framework.Assert;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import static com.mparticle.internal.Logger.*;
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

    class MockAppboyKit extends AppboyKit {
        final String[] calledAuthority = new String[1];

        MockAppboyKit() {
            setKitManager(new MockKitManagerImpl(Mockito.mock(Context.class), Mockito.mock(ReportingManager.class), new MockCoreCallbacks()));
        }

        @Override
        protected void setAuthority(String authority) {
            calledAuthority[0] = authority;
        }

        @Override
        void queueDataFlush() {
            //do nothing
        }
    }

    class MockContextApplication extends Application {
        @Override
        public Context getApplicationContext() {
            return this;
        }

        @Override
        public SharedPreferences getSharedPreferences(String name, int mode) {
            return new MockSharedPreferences();
        }

        @Override
        public void registerActivityLifecycleCallbacks(ActivityLifecycleCallbacks callback) {
            //do nothing
        }
    }

    class MockSharedPreferences implements SharedPreferences, SharedPreferences.Editor {


        @Override
        public Map<String, ?> getAll() {
            return null;
        }

        @Nullable
        @Override
        public String getString(String key, @Nullable String defValue) {
            return "";
        }

        @Nullable
        @Override
        public Set<String> getStringSet(String key, @Nullable Set<String> defValues) {
            return new TreeSet<>();
        }

        @Override
        public int getInt(String key, int defValue) {
            return 0;
        }

        @Override
        public long getLong(String key, long defValue) {
            return 0;
        }

        @Override
        public float getFloat(String key, float defValue) {
            return 0;
        }

        @Override
        public boolean getBoolean(String key, boolean defValue) {
            return false;
        }

        @Override
        public boolean contains(String key) {
            return false;
        }

        @Override
        public Editor edit() {
            return this;
        }

        @Override
        public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {

        }

        @Override
        public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {

        }

        @Override
        public Editor putString(String key, @Nullable String value) {
            return this;
        }

        @Override
        public Editor putStringSet(String key, @Nullable Set<String> values) {
            return this;
        }

        @Override
        public Editor putInt(String key, int value) {
            return this;
        }

        @Override
        public Editor putLong(String key, long value) {
            return this;
        }

        @Override
        public Editor putFloat(String key, float value) {
            return this;
        }

        @Override
        public Editor putBoolean(String key, boolean value) {
            return this;
        }

        @Override
        public Editor remove(String key) {
            return this;
        }

        @Override
        public Editor clear() {
            return this;
        }

        @Override
        public boolean commit() {
            return false;
        }

        @Override
        public void apply() {

        }
    }

    class MockUser implements MParticleUser {
        Map<MParticle.IdentityType, String> identities;

        MockUser(Map<MParticle.IdentityType, String> identities) {
            this.identities = identities;
        }

        @NonNull
        @Override
        public long getId() {
            return 0;
        }

        @NonNull
        @Override
        public Cart getCart() {
            return null;
        }

        @NonNull
        @Override
        public Map<String, Object> getUserAttributes() {
            return null;
        }

        @Nullable
        @Override
        public Map<String, Object> getUserAttributes(@Nullable UserAttributeListener userAttributeListener) {
            return null;
        }

        @Override
        public boolean setUserAttributes(@NonNull Map<String, Object> map) {
            return false;
        }

        @Override
        public Map<MParticle.IdentityType, String> getUserIdentities() {
            return identities;
        }

        @Override
        public boolean setUserAttribute(@NonNull String s, @NonNull Object o) {
            return false;
        }

        @Override
        public boolean setUserAttributeList(@NonNull String s, @NonNull Object o) {
            return false;
        }

        @Override
        public boolean incrementUserAttribute(@NonNull String s, int i) {
            return false;
        }

        @Override
        public boolean removeUserAttribute(@NonNull String s) {
            return false;
        }

        @Override
        public boolean setUserTag(@NonNull String s) {
            return false;
        }

        @NonNull
        @Override
        public ConsentState getConsentState() {
            return null;
        }

        @Override
        public void setConsentState(@Nullable ConsentState consentState) {

        }


        public boolean isLoggedIn() {
            return false;
        }

        @Override
        public long getFirstSeenTime() {
            return 0;
        }

        @Override
        public long getLastSeenTime() {
            return 0;
        }
    }


    public class MockKitManagerImpl extends KitManagerImpl {

        public MockKitManagerImpl() {
            this(new MockContext(), Mockito.mock(ReportingManager.class), Mockito.mock(CoreCallbacks.class));
            Mockito.when(mCoreCallbacks.getKitListener()).thenReturn(CoreCallbacks.KitListener.EMPTY);
        }

        public MockKitManagerImpl(Context context, ReportingManager reportingManager, CoreCallbacks coreCallbacks) {
            super(context, reportingManager, coreCallbacks, new BackgroundTaskHandler() {
                @Override
                public void executeNetworkRequest(Runnable runnable) {

                }
            });
        }

        @Override
        protected KitConfiguration createKitConfiguration(JSONObject configuration) throws JSONException {
            return MockKitConfiguration.createKitConfiguration(configuration);
        }

        @Override
        public int getUserBucket() {
            return 50;
        }
    }

    class MockContext extends Context {


        SharedPreferences sharedPreferences = new MockSharedPreferences();
        Resources resources = new MockResources();
        MockApplication application = null;

        public void setSharedPreferences(SharedPreferences prefs){
            sharedPreferences = prefs;
        }

        @Override
        public Context getApplicationContext() {
            if (application == null){
                application = new MockApplication(this);
            }
            return application;
        }


        @Override
        public int checkCallingOrSelfPermission(String permission) {
            return PackageManager.PERMISSION_GRANTED;
        }


        @Override
        public SharedPreferences getSharedPreferences(String name, int mode) {
            return sharedPreferences;
        }

        @Override
        public Resources getResources() {
            return resources;
        }

        @Override
        public Object getSystemService(String name) {
            if (name.equals(Context.TELEPHONY_SERVICE)){
                return Mockito.mock(TelephonyManager.class);
            }
            return null;
        }


        @Override
        public PackageManager getPackageManager() {
            PackageManager manager = Mockito.mock(PackageManager.class);
            PackageInfo info = Mockito.mock(PackageInfo.class);
            info.versionName = "42";
            info.versionCode = 42;
            ApplicationInfo appInfo = Mockito.mock(ApplicationInfo.class);
            try {
                Mockito.when(manager.getPackageInfo(Mockito.anyString(), Mockito.anyInt())).thenReturn(info);
                Mockito.when(manager.getInstallerPackageName(Mockito.anyString())).thenReturn("com.mparticle.test.installer");

                Mockito.when(manager.getApplicationInfo(Mockito.anyString(), Mockito.anyInt())).thenReturn(appInfo);
                Mockito.when(manager.getApplicationLabel(appInfo)).thenReturn("test label");
            }catch (Exception e){
                Assert.fail(e.toString());
            }
            return manager;
        }

        @Override
        public String getPackageName() {
            return "com.mparticle.test";
        }

        @Override
        public ApplicationInfo getApplicationInfo() {
            return new ApplicationInfo();
        }


        /**
         * Stubbed methods
         */


        @Override
        public void setTheme(int resid) {

        }

        @Override
        public Resources.Theme getTheme() {
            return null;
        }

        @Override
        public ClassLoader getClassLoader() {
            return null;
        }


        @Override
        public void sendBroadcast(Intent intent) {

        }

        @Override
        public void sendBroadcast(Intent intent, @Nullable String receiverPermission) {

        }

        @Override
        public void sendOrderedBroadcast(Intent intent, @Nullable String receiverPermission) {

        }

        @Override
        public void sendOrderedBroadcast(@NonNull Intent intent, @Nullable String receiverPermission, @Nullable BroadcastReceiver resultReceiver, @Nullable Handler scheduler, int initialCode, @Nullable String initialData, @Nullable Bundle initialExtras) {

        }

        @Override
        public void sendBroadcastAsUser(Intent intent, UserHandle user) {

        }

        @Override
        public void sendBroadcastAsUser(Intent intent, UserHandle user, @Nullable String receiverPermission) {

        }

        @Override
        public void sendOrderedBroadcastAsUser(Intent intent, UserHandle user, @Nullable String receiverPermission, BroadcastReceiver resultReceiver, @Nullable Handler scheduler, int initialCode, @Nullable String initialData, @Nullable Bundle initialExtras) {

        }

        @Override
        public void sendStickyBroadcast(Intent intent) {

        }

        @Override
        public void sendStickyOrderedBroadcast(Intent intent, BroadcastReceiver resultReceiver, @Nullable Handler scheduler, int initialCode, @Nullable String initialData, @Nullable Bundle initialExtras) {

        }

        @Override
        public void removeStickyBroadcast(Intent intent) {

        }

        @Override
        public void sendStickyBroadcastAsUser(Intent intent, UserHandle user) {

        }

        @Override
        public void sendStickyOrderedBroadcastAsUser(Intent intent, UserHandle user, BroadcastReceiver resultReceiver, @Nullable Handler scheduler, int initialCode, @Nullable String initialData, @Nullable Bundle initialExtras) {

        }

        @Override
        public void removeStickyBroadcastAsUser(Intent intent, UserHandle user) {

        }

        @Nullable
        @Override
        public Intent registerReceiver(@Nullable BroadcastReceiver receiver, IntentFilter filter) {
            return null;
        }

        @Nullable
        @Override
        public Intent registerReceiver(@Nullable BroadcastReceiver receiver, IntentFilter filter, int flags) {
            return null;
        }

        @Nullable
        @Override
        public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter, @Nullable String broadcastPermission, @Nullable Handler scheduler) {
            return null;
        }

        @Nullable
        @Override
        public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter, @Nullable String broadcastPermission, @Nullable Handler scheduler, int flags) {
            return null;
        }

        @Override
        public void unregisterReceiver(BroadcastReceiver receiver) {

        }

        @Nullable
        @Override
        public ComponentName startService(Intent service) {
            return null;
        }

        @Nullable
        @Override
        public ComponentName startForegroundService(Intent service) {
            return null;
        }

        @Override
        public boolean stopService(Intent service) {
            return false;
        }

        @Override
        public boolean bindService(Intent service, @NonNull ServiceConnection conn, int flags) {
            return false;
        }

        @Override
        public void unbindService(@NonNull ServiceConnection conn) {

        }

        @Override
        public boolean startInstrumentation(@NonNull ComponentName className, @Nullable String profileFile, @Nullable Bundle arguments) {
            return false;
        }

        @Override
        public int checkSelfPermission(@NonNull String permission) {
            return 0;
        }

        @Override
        public void enforcePermission(@NonNull String permission, int pid, int uid, @Nullable String message) {

        }

        @Override
        public void enforceCallingPermission(@NonNull String permission, @Nullable String message) {

        }

        @Override
        public void enforceCallingOrSelfPermission(@NonNull String permission, @Nullable String message) {

        }

        @Override
        public void grantUriPermission(String toPackage, Uri uri, int modeFlags) {

        }

        @Override
        public void revokeUriPermission(Uri uri, int modeFlags) {

        }

        @Override
        public void revokeUriPermission(String toPackage, Uri uri, int modeFlags) {

        }

        @Override
        public int checkUriPermission(Uri uri, int pid, int uid, int modeFlags) {
            return 0;
        }

        @Override
        public int checkCallingUriPermission(Uri uri, int modeFlags) {
            return 0;
        }

        @Override
        public int checkCallingOrSelfUriPermission(Uri uri, int modeFlags) {
            return 0;
        }

        @Override
        public int checkUriPermission(@Nullable Uri uri, @Nullable String readPermission, @Nullable String writePermission, int pid, int uid, int modeFlags) {
            return 0;
        }

        @Override
        public void enforceUriPermission(Uri uri, int pid, int uid, int modeFlags, String message) {

        }

        @Override
        public void enforceCallingUriPermission(Uri uri, int modeFlags, String message) {

        }

        @Override
        public void enforceCallingOrSelfUriPermission(Uri uri, int modeFlags, String message) {

        }

        @Override
        public void enforceUriPermission(@Nullable Uri uri, @Nullable String readPermission, @Nullable String writePermission, int pid, int uid, int modeFlags, @Nullable String message) {

        }

        @Override
        public Context createPackageContext(String packageName, int flags) throws PackageManager.NameNotFoundException {
            return null;
        }

        @Override
        public Context createContextForSplit(String splitName) throws PackageManager.NameNotFoundException {
            return null;
        }

        @Override
        public Context createConfigurationContext(@NonNull Configuration overrideConfiguration) {
            return null;
        }

        @Override
        public Context createDisplayContext(@NonNull Display display) {
            return null;
        }

        @Override
        public Context createDeviceProtectedStorageContext() {
            return null;
        }

        @Override
        public boolean isDeviceProtectedStorage() {
            return false;
        }

        @Override
        public boolean moveSharedPreferencesFrom(Context sourceContext, String name) {
            return false;
        }

        @Override
        public boolean deleteSharedPreferences(String name) {
            return false;
        }

        @Override
        public FileInputStream openFileInput(String name) throws FileNotFoundException {
            return null;
        }

        @Override
        public FileOutputStream openFileOutput(String name, int mode) throws FileNotFoundException {
            return null;
        }

        @Override
        public boolean deleteFile(String name) {
            return false;
        }

        @Override
        public File getFileStreamPath(String name) {
            return null;
        }

        @Override
        public File getDataDir() {
            return null;
        }

        @Override
        public File getFilesDir() {
            return null;
        }

        @Override
        public File getNoBackupFilesDir() {
            return null;
        }

        @Nullable
        @Override
        public File getExternalFilesDir(@Nullable String type) {
            return null;
        }

        @Override
        public File[] getExternalFilesDirs(String type) {
            return new File[0];
        }

        @Override
        public File getObbDir() {
            return null;
        }

        @Override
        public File[] getObbDirs() {
            return new File[0];
        }

        @Override
        public File getCacheDir() {
            return null;
        }

        @Override
        public File getCodeCacheDir() {
            return null;
        }

        @Nullable
        @Override
        public File getExternalCacheDir() {
            return null;
        }

        @Override
        public File[] getExternalCacheDirs() {
            return new File[0];
        }

        @Override
        public File[] getExternalMediaDirs() {
            return new File[0];
        }

        @Override
        public String[] fileList() {
            return new String[0];
        }

        @Override
        public File getDir(String name, int mode) {
            return null;
        }

        @Override
        public SQLiteDatabase openOrCreateDatabase(String name, int mode, SQLiteDatabase.CursorFactory factory) {
            return null;
        }

        @Override
        public SQLiteDatabase openOrCreateDatabase(String name, int mode, SQLiteDatabase.CursorFactory factory, @Nullable DatabaseErrorHandler errorHandler) {
            return null;
        }

        @Override
        public boolean moveDatabaseFrom(Context sourceContext, String name) {
            return false;
        }

        @Override
        public boolean deleteDatabase(String name) {
            return false;
        }

        @Override
        public File getDatabasePath(String name) {
            return null;
        }

        @Override
        public String[] databaseList() {
            return new String[0];
        }

        @Override
        public Drawable getWallpaper() {
            return null;
        }

        @Override
        public Drawable peekWallpaper() {
            return null;
        }

        @Override
        public int getWallpaperDesiredMinimumWidth() {
            return 0;
        }

        @Override
        public int getWallpaperDesiredMinimumHeight() {
            return 0;
        }

        @Override
        public void setWallpaper(Bitmap bitmap) throws IOException {

        }

        @Override
        public void setWallpaper(InputStream data) throws IOException {

        }

        @Override
        public void clearWallpaper() throws IOException {

        }

        @Override
        public void startActivity(Intent intent) {

        }

        @Override
        public void startActivity(Intent intent, @Nullable Bundle options) {

        }

        @Override
        public void startActivities(Intent[] intents) {

        }

        @Override
        public void startActivities(Intent[] intents, Bundle options) {

        }

        @Override
        public void startIntentSender(IntentSender intent, @Nullable Intent fillInIntent, int flagsMask, int flagsValues, int extraFlags) throws IntentSender.SendIntentException {

        }

        @Override
        public void startIntentSender(IntentSender intent, @Nullable Intent fillInIntent, int flagsMask, int flagsValues, int extraFlags, @Nullable Bundle options) throws IntentSender.SendIntentException {

        }

        @Nullable
        @Override
        public String getSystemServiceName(@NonNull Class<?> serviceClass) {
            return null;
        }

        @Override
        public int checkPermission(@NonNull String permission, int pid, int uid) {
            return 0;
        }

        @Override
        public int checkCallingPermission(@NonNull String permission) {
            return 0;
        }


        @Override
        public ContentResolver getContentResolver() {
            return null;
        }

        @Override
        public Looper getMainLooper() {
            return null;
        }

        @Override
        public String getPackageResourcePath() {
            return null;
        }

        @Override
        public String getPackageCodePath() {
            return null;
        }

        @Override
        public AssetManager getAssets() {
            return null;
        }
    }

    class MockApplication extends Application {
        MockContext mContext;
        public ActivityLifecycleCallbacks mCallbacks;

        public MockApplication(MockContext context) {
            super();
            mContext = context;
        }

        @Override
        public void registerActivityLifecycleCallbacks(ActivityLifecycleCallbacks callback) {
            mCallbacks = callback;
        }

        @Override
        public Context getApplicationContext() {
            return this;
        }

        public void setSharedPreferences(SharedPreferences prefs){
            mContext.setSharedPreferences(prefs);
        }

        @Override
        public Object getSystemService(String name) {
            return mContext.getSystemService(name);
        }

        @Override
        public SharedPreferences getSharedPreferences(String name, int mode) {
            return mContext.getSharedPreferences(name, mode);
        }

        @Override
        public PackageManager getPackageManager() {
            return mContext.getPackageManager();
        }

        @Override
        public String getPackageName() {
            return mContext.getPackageName();
        }

        @Override
        public ApplicationInfo getApplicationInfo() {
            return mContext.getApplicationInfo();
        }

        @Override
        public Resources getResources() {
            return mContext.getResources();
        }
    }

    static class MockKitConfiguration extends KitConfiguration {

        @Override
        public KitConfiguration parseConfiguration(JSONObject json) throws JSONException {
            mTypeFilters = new MockSparseBooleanArray();
            mNameFilters = new MockSparseBooleanArray();
            mAttributeFilters = new MockSparseBooleanArray();
            mScreenNameFilters = new MockSparseBooleanArray();
            mScreenAttributeFilters = new MockSparseBooleanArray();
            mUserIdentityFilters = new MockSparseBooleanArray();
            mUserAttributeFilters = new MockSparseBooleanArray();
            mCommerceAttributeFilters = new MockSparseBooleanArray();
            mCommerceEntityFilters = new MockSparseBooleanArray();
            return super.parseConfiguration(json);
        }

        public static KitConfiguration createKitConfiguration(JSONObject json) throws JSONException{
            return new MockKitConfiguration().parseConfiguration(json);
        }

        public static KitConfiguration createKitConfiguration() throws JSONException{
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("id", 42);
            return new MockKitConfiguration().parseConfiguration(jsonObject);
        }

        @Override
        protected SparseBooleanArray convertToSparseArray(JSONObject json) {
            SparseBooleanArray map = new MockSparseBooleanArray();
            for (Iterator<String> iterator = json.keys(); iterator.hasNext();) {
                try {
                    String key = iterator.next();
                    map.put(Integer.parseInt(key), json.getInt(key) == 1);
                }catch (JSONException jse){
                    error("Issue while parsing kit configuration: " + jse.getMessage());
                }
            }
            return map;
        }
        class MockSparseBooleanArray extends SparseBooleanArray {
            @Override
            public boolean get(int key) {
                return get(key, false);
            }

            @Override
            public boolean get(int key, boolean valueIfKeyNotFound) {
                System.out.print("SparseArray getting: " + key);
                if (map.containsKey(key)) {
                    return map.get(key);
                }else{
                    return valueIfKeyNotFound;
                }
            }

            Map<Integer, Boolean> map = new HashMap<Integer, Boolean>();
            @Override
            public void put(int key, boolean value) {
                map.put(key, value);
            }

            @Override
            public void clear() {
                map.clear();
            }

            @Override
            public int size() {
                return map.size();
            }

            @Override
            public String toString() {
                return map.toString();
            }
        }
    }

    static class MockResources extends Resources {
        public static String TEST_APP_KEY = "the app key";
        public static String TEST_APP_SECRET = "the app secret";


        public MockResources() {
            super(null, null, null);
        }

        @Override
        public int getIdentifier(String name, String defType, String defPackage) {
            if (name.equals("mp_key")){
                return 1;
            }else if (name.equals("mp_secret")){
                return 2;
            }

            return 0;
        }

        @Override
        public String getString(int id) throws NotFoundException {
            switch (id){
                case 1:
                    return TEST_APP_KEY;
                case 2:
                    return TEST_APP_SECRET;

            }
            return null;
        }

        @Override
        public String getString(int id, Object... formatArgs) throws NotFoundException {
            return super.getString(id, formatArgs);
        }
    }

    class MockCoreCallbacks implements CoreCallbacks {

        @Override
        public boolean isBackgrounded() {
            return false;
        }

        @Override
        public int getUserBucket() {
            return 0;
        }

        @Override
        public boolean isEnabled() {
            return false;
        }

        @Override
        public void setIntegrationAttributes(int kitId, Map<String, String> integrationAttributes) {

        }

        @Override
        public Map<String, String> getIntegrationAttributes(int kitId) {
            return null;
        }

        @Override
        public WeakReference<Activity> getCurrentActivity() {
            return null;
        }

        @Override
        public JSONArray getLatestKitConfiguration() {
            return null;
        }

        @Override
        public boolean isPushEnabled() {
            return false;
        }

        @Override
        public String getPushSenderId() {
            return null;
        }

        @Override
        public String getPushInstanceId() {
            return null;
        }

        @Override
        public Uri getLaunchUri() {
            return null;
        }

        @Override
        public String getLaunchAction() {
            return null;
        }

        @Override
        public void replayAndDisableQueue() { }

        @Override
        public KitListener getKitListener() {
            return KitListener.EMPTY;
        }
    }


}