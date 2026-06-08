package dev.zm.rankup.config;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class PlaceholderContext {

    private static final PlaceholderContext EMPTY = new PlaceholderContext(Collections.emptyMap());

    private final Map<String, String> values;

    private PlaceholderContext(Map<String, String> values) {
        this.values = values.isEmpty()
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    public static PlaceholderContext empty() {
        return EMPTY;
    }

    public static PlaceholderContext of(String key, String value) {
        if (key == null || key.isBlank() || value == null) {
            return EMPTY;
        }
        Map<String, String> values = new LinkedHashMap<>();
        values.put(key, value);
        return new PlaceholderContext(values);
    }

    public static PlaceholderContext of(Map<String, String> values) {
        if (values == null || values.isEmpty()) {
            return EMPTY;
        }
        return new PlaceholderContext(values);
    }

    public static PlaceholderContext withPosition(Integer position) {
        if (position == null) {
            return EMPTY;
        }
        return of("position", String.valueOf(position));
    }

    public PlaceholderContext with(String key, String value) {
        if (key == null || key.isBlank() || value == null) {
            return this;
        }
        Map<String, String> merged = new LinkedHashMap<>(values);
        merged.put(key, value);
        return new PlaceholderContext(merged);
    }

    public Map<String, String> values() {
        return values;
    }

    public String get(String key) {
        return values.get(key);
    }
}
