package com.mparticle.kits;


import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.Nullable;
import android.test.mock.MockApplication;
import android.test.mock.MockContext;

import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class AppboyKitTests {

    private KitIntegration getKit() {
        return new AppboyKit();
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
            kit.onKitCreate(settings, Mockito.mock(Context.class));
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



    class MockAppboyKit extends AppboyKit {
        final String[] calledAuthority = new String[1];

        @Override
        protected void setAuthority(String authority) {
            calledAuthority[0] = authority;
        }

        @Override
        void queueDataFlush() {
            //do nothing
        }
    }

    class MockContextApplication extends MockApplication {
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

}