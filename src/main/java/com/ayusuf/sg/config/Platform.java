package com.ayusuf.sg.config;

import java.util.Arrays;
import java.util.Locale;

public enum Platform {
    DESKTOP,
    ANDROID;

    public static Platform from(String value) {
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Unknown platform '" + value + "'. Use one of " + Arrays.toString(values()));
        }
    }

    public boolean isMobile() {
        return this == ANDROID;
    }
}
