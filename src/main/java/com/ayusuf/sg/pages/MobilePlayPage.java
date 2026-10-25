package com.ayusuf.sg.pages;

import com.ayusuf.sg.config.Config;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/** /play on a mobile browser: app store links instead of the WebGL game. */
public class MobilePlayPage extends BasePage {

    public static final String ANDROID_APP_ID = "com.kitkagames.fallbuddies";
    public static final String IOS_APP_ID = "id1541153375";

    private static final By GOOGLE_PLAY = By.cssSelector("a[href*='play.google.com/store/apps/details?id=" + ANDROID_APP_ID + "']");
    private static final By APP_STORE = By.cssSelector("a[href*='apps.apple.com'][href*='" + IOS_APP_ID + "']");
    private static final By GAME_CANVAS = By.cssSelector("canvas#react-unity-webgl-canvas-1");

    public MobilePlayPage(WebDriver driver) {
        super(driver);
    }

    public MobilePlayPage open() {
        driver.get(Config.baseUrl() + "/play");
        new CookieBanner(driver).denyIfPresent();
        return this;
    }

    public boolean showsGooglePlayLink() {
        return waitUntilVisible(GOOGLE_PLAY);
    }

    public boolean showsAppStoreLink() {
        return waitUntilVisible(APP_STORE);
    }

    public boolean hasGameCanvas() {
        return !driver.findElements(GAME_CANVAS).isEmpty();
    }
}
