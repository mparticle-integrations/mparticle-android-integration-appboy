package com.mparticle.kits.mocks;

import android.content.SharedPreferences;
import android.support.annotation.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class MockSharedPreferences implements SharedPreferences, SharedPreferences.Editor {


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
