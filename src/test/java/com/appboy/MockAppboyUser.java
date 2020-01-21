package com.appboy;

import com.appboy.enums.Month;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import bo.app.bs;
import bo.app.bv;
import bo.app.dz;
import bo.app.ec;

public class MockAppboyUser extends AppboyUser {
    public int dobYear = -1;
    public Month dobMonth = null;
    public int dobDay = -1;

    MockAppboyUser() {
        super(null, null, null, null, null);
    }

    public boolean setDateOfBirth(int year, Month month, int day) {
        dobYear = year;
        dobMonth = month;
        dobDay = day;
        return true;
    }

    private Map<String, List<String>> customAttributeArray = new HashMap<>();
    private Map<String, String> customAttributes = new HashMap<>();

    public Map<String, List<String>> getCustomAttributeArray() {
        return customAttributeArray;
    }

    @Override
    public boolean addToCustomAttributeArray(String key, String value) {
        List<String> customArray = customAttributeArray.get(key);
        if (customArray == null) {
            customArray = new ArrayList<>();
        }
        customArray.add(value);
        customAttributeArray.put(key, customArray);
        return true;
    }

    @Override
    public boolean removeFromCustomAttributeArray(String key, String value) {
        try {
            customAttributeArray.get(key).remove(value);
            return true;
        } catch (NullPointerException npe) {
            return false;
        }
    }

    @Override
    public boolean setCustomUserAttribute(String key, String value) {
        customAttributes.put(key, value);
        return true;
    }

    public Map<String, String> getCustomUserAttributes() {
        return customAttributes;
    }
}
