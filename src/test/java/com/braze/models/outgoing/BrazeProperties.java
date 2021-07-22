package com.braze.models.outgoing;

import com.appboy.models.outgoing.AppboyProperties;

import java.util.HashMap;
import java.util.Map;

public class BrazeProperties extends AppboyProperties {
    private Map<String, Object> properties = new HashMap<>();

    public BrazeProperties() {
    }

    public AppboyProperties addProperty(String key, long value) {
        properties.put(key, value);
        return this;
    }

    public AppboyProperties addProperty(String key, int value) {
        properties.put(key, value);
        return this;
    }

    public AppboyProperties addProperty(String key, String value) {
        properties.put(key, value);
        return this;
    }

    public AppboyProperties addProperty(String key, double value) {
        properties.put(key, value);
        return this;
    }

    public AppboyProperties addProperty(String key, boolean value) {
        properties.put(key, value);
        return this;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }
}
