package com.appboy.models.outgoing;

import java.util.HashMap;
import java.util.Map;

public class AppboyProperties {
    private Map<String, Object> properties = new HashMap<>();

    public AppboyProperties() {
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
