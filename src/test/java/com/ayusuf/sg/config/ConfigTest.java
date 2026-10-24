package com.ayusuf.sg.config;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Test(groups = "unit")
public class ConfigTest {

    @AfterMethod(alwaysRun = true)
    public void clear() {
        System.clearProperty("timeout.seconds");
        System.clearProperty("base.url");
    }

    public void mapsKeysToEnvNames() {
        assertThat(Config.toEnvName("base.url")).isEqualTo("BASE_URL");
        assertThat(Config.toEnvName("android.platform.version")).isEqualTo("ANDROID_PLATFORM_VERSION");
    }

    public void readsDefaults() {
        assertThat(Config.find("android.device.name")).contains("Android Emulator");
    }

    public void systemPropertyWinsOverDefault() {
        System.setProperty("timeout.seconds", "5");
        assertThat(Config.timeout().toSeconds()).isEqualTo(5);
    }

    public void trimsTrailingSlashFromBaseUrl() {
        System.setProperty("base.url", "https://example.com/");
        assertThat(Config.baseUrl()).isEqualTo("https://example.com");
    }

    public void blankValuesCountAsMissing() {
        assertThat(Config.find("appium.url")).isEmpty();
        assertThatThrownBy(() -> Config.get("does.not.exist"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DOES_NOT_EXIST");
    }

    public void rejectsUnknownPlatform() {
        assertThatThrownBy(() -> Platform.from("ios"))
                .hasMessageContaining("Unknown platform 'ios'");
    }
}
