package com.ayusuf.sg.config;

import io.github.cdimascio.dotenv.Dotenv;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;

/** Precedence: -D > env var > .env > default.properties. "base.url" maps to BASE_URL. */
public final class Config {

    private static final Properties DEFAULTS = loadDefaults();
    private static final Dotenv DOTENV = Dotenv.configure().ignoreIfMissing().ignoreIfMalformed().load();

    private Config() {
    }

    public static Optional<String> find(String key) {
        String value = System.getProperty(key);
        if (isBlank(value)) value = System.getenv(toEnvName(key));
        if (isBlank(value)) value = DOTENV.get(toEnvName(key), null);
        if (isBlank(value)) value = DEFAULTS.getProperty(key);
        return isBlank(value) ? Optional.empty() : Optional.of(value.trim());
    }

    public static String get(String key) {
        return find(key).orElseThrow(() -> new IllegalStateException(
                "Missing config '" + key + "'. Set it in .env (" + toEnvName(key) + "=...) or pass -D" + key + "=..."));
    }

    public static boolean getBool(String key) {
        return Boolean.parseBoolean(get(key));
    }

    public static int getInt(String key) {
        return Integer.parseInt(get(key));
    }

    public static Platform platform() {
        return Platform.from(get("platform"));
    }

    public static String baseUrl() {
        return get("base.url").replaceAll("/+$", "");
    }

    public static Duration timeout() {
        return Duration.ofSeconds(getInt("timeout.seconds"));
    }

    static String toEnvName(String key) {
        return key.replace('.', '_').toUpperCase(Locale.ROOT);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static Properties loadDefaults() {
        Properties props = new Properties();
        try (InputStream in = Config.class.getResourceAsStream("/config/default.properties")) {
            if (in != null) props.load(in);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read default.properties", e);
        }
        return props;
    }
}
