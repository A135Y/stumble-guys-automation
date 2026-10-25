package com.ayusuf.sg.driver;

import com.ayusuf.sg.config.Config;
import com.ayusuf.sg.config.Platform;
import com.ayusuf.sg.support.CursorOverlay;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.net.URL;
import java.time.Duration;

public final class DriverFactory {

    private DriverFactory() {
    }

    public static WebDriver create(Platform platform) {
        return switch (platform) {
            case DESKTOP -> desktopChrome();
            case ANDROID -> androidChrome();
        };
    }

    private static WebDriver desktopChrome() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--disable-notifications", "--lang=en-US");
        if (Config.getBool("headless")) {
            options.addArguments("--headless=new");
        }

        ChromeDriver driver = new ChromeDriver(options);
        if (Config.getBool("window.maximized")) {
            driver.manage().window().maximize();
        } else {
            driver.manage().window().setSize(new Dimension(Config.getInt("window.width"), Config.getInt("window.height")));
        }
        if (Config.getBool("show.cursor")) CursorOverlay.install(driver);
        return driver;
    }

    private static WebDriver androidChrome() {
        UiAutomator2Options options = new UiAutomator2Options()
                .withBrowserName("Chrome")
                .setDeviceName(Config.get("android.device.name"))
                .setNewCommandTimeout(Duration.ofMinutes(2))
                .setAvdLaunchTimeout(Duration.ofMinutes(4))
                .setAvdReadyTimeout(Duration.ofMinutes(4));
        options.setCapability("appium:chromedriverAutodownload", true);
        Config.find("android.platform.version").ifPresent(options::setPlatformVersion);
        Config.find("android.avd").ifPresent(options::setAvd);

        URL server = AppiumServer.url();
        return new AndroidDriver(server, options);
    }
}
