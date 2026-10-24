package com.ayusuf.sg.driver;

import com.ayusuf.sg.config.Config;
import io.appium.java_client.service.local.AppiumDriverLocalService;
import io.appium.java_client.service.local.AppiumServiceBuilder;
import io.appium.java_client.service.local.flags.GeneralServerFlag;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

/** Uses APPIUM_URL if set, otherwise starts one local server for the whole run. */
public final class AppiumServer {

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
            service = new AppiumServiceBuilder()
                    .usingAnyFreePort()
                    .withArgument(GeneralServerFlag.ALLOW_INSECURE, "uiautomator2:chromedriver_autodownload")
                    .withArgument(GeneralServerFlag.LOG_LEVEL, "warn")
                    .build();
            service.start();
            Runtime.getRuntime().addShutdownHook(new Thread(AppiumServer::stop));
        }
        return service.getUrl();
    }

    public static synchronized void stop() {
        if (service != null && service.isRunning()) {
            service.stop();
        }
        service = null;
    }
}
