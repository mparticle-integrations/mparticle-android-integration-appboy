package com.braze.models.outgoing;

import java.util.HashMap;
import java.util.Map;

public class BrazeProperties  {
    private Map<String, Object> properties = new HashMap<>();

    public BrazeProperties() {
    }

    public BrazeProperties addProperty(String key, long value) {
        properties.put(key, value);
        return this;
    }

    public BrazeProperties addProperty(String key, int value) {
        properties.put(key, value);
        return this;
    }

    public BrazeProperties addProperty(String key, String value) {
        properties.put(key, value);
        return this;
    }

    public BrazeProperties addProperty(String key, double value) {
        properties.put(key, value);
        return this;
    }

    public BrazeProperties addProperty(String key, boolean value) {
        properties.put(key, value);
        return this;
    }

    public BrazeProperties addProperty(String key, Object value) {
        properties.put(key, value);
        return this;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }
}
