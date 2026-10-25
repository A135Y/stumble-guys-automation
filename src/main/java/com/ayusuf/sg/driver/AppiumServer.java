package com.ayusuf.sg.driver;

import com.ayusuf.sg.config.Config;
import io.appium.java_client.service.local.AppiumDriverLocalService;
import io.appium.java_client.service.local.AppiumServiceBuilder;
import io.appium.java_client.service.local.flags.GeneralServerFlag;

import com.ayusuf.sg.support.Artifacts;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Uses APPIUM_URL if set, otherwise starts one local server for the whole run: the Appium pinned in package.json
 * (npm ci), with APPIUM_HOME pointing at the repo so it finds the pinned UiAutomator2 driver.
 */
public final class AppiumServer {

    private static final Path REPO = Path.of("").toAbsolutePath();
    private static AppiumDriverLocalService service;

    private AppiumServer() {
    }

    public static synchronized URL url() {
        var external = Config.find("appium.url");
        if (external.isPresent()) {
            try {
                return URI.create(external.get()).toURL();
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException("Bad APPIUM_URL: " + external.get(), e);
            }
        }
        if (service == null) {
            AppiumServiceBuilder builder = new AppiumServiceBuilder()
                    .usingAnyFreePort()
                    .withEnvironment(Map.of("APPIUM_HOME", REPO.toString(), "ANDROID_HOME", androidHome()))
                    .withArgument(GeneralServerFlag.ALLOW_INSECURE, "uiautomator2:chromedriver_autodownload")
                    .withArgument(GeneralServerFlag.LOG_LEVEL, "info")
                    .withLogFile(Artifacts.RUN_DIR.resolve("appium.log").toFile());
            File pinned = REPO.resolve("node_modules/appium/index.js").toFile();
            if (pinned.exists()) builder.withAppiumJS(pinned);
            Artifacts.RUN_DIR.toFile().mkdirs();
            service = builder.build();
            service.clearOutPutStreams(); // log goes to appium.log only
            service.start();
            Runtime.getRuntime().addShutdownHook(new Thread(AppiumServer::stop));
        }
        return service.getUrl();
    }

    /** ANDROID_HOME, else the Homebrew or Android Studio default location. */
    static String androidHome() {
        return Config.find("android.home")
                .or(() -> Stream.of("/opt/homebrew/share/android-commandlinetools", "/usr/local/share/android-commandlinetools",
                                System.getProperty("user.home") + "/Library/Android/sdk")
                        .filter(d -> new File(d, "platform-tools").isDirectory()).findFirst())
                .orElseThrow(() -> new IllegalStateException(
                        "Android SDK not found. Run scripts/setup-android.sh, or set ANDROID_HOME"));
    }

    public static synchronized void stop() {
        if (service != null && service.isRunning()) {
            service.stop();
        }
        service = null;
    }
}
