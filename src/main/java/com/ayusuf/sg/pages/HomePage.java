package com.ayusuf.sg.pages;

import com.ayusuf.sg.config.Config;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class HomePage extends BasePage {

    private static final By PLAY_LINK = By.cssSelector("a[href='/play']");
    private static final By MENU_BUTTON = By.xpath("//button[.//img[contains(@src,'/images/menu/')]]");
    private static final By LOGIN_BUTTON = By.cssSelector("button[class*='AuthButton_login']");
    private static final By LOGGED_OUT_ICON = By.cssSelector("img[src*='/images/menu/logged_out']");

    public HomePage(WebDriver driver) {
        super(driver);
    }

    public HomePage open() {
        driver.get(Config.baseUrl() + "/");
        new CookieBanner(driver).denyIfPresent();
        return this;
    }

    public String title() {
        return driver.getTitle();
    }

    public boolean isPlayLinkVisible() {
        return waitUntilVisible(PLAY_LINK);
    }

    public HomePage openMenu() {
        click(MENU_BUTTON);
        return this;
    }

    public boolean isLoginButtonVisible() {
        return waitUntilVisible(LOGIN_BUTTON);
    }

    public boolean isLoggedOut() {
        return !driver.findElements(LOGGED_OUT_ICON).isEmpty();
    }
}
