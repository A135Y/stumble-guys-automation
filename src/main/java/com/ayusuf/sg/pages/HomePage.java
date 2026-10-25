package com.ayusuf.sg.pages;

import com.ayusuf.sg.config.Config;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class HomePage extends BasePage {

    private static final By PLAY_LINK = By.cssSelector("a[href='/play']");
    private static final By MENU_BUTTON = By.xpath("//button[.//img[contains(@src,'/images/menu/')]]");
    private static final By LOGIN_BUTTON = By.cssSelector("button[class*='AuthButton_login']");
    private static final By LOGGED_OUT_ICON = By.cssSelector("img[src*='/images/menu/logged_out']");
    private static final By SHOP_LINK = By.cssSelector("header a[href='/shop'], nav a[href='/shop']");

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

    public LoginModal clickLogin() {
        click(LOGIN_BUTTON);
        return new LoginModal(driver);
    }

    public boolean isLoggedOut() {
        return !driver.findElements(LOGGED_OUT_ICON).isEmpty();
    }

    public boolean hasScopelyToken() {
        return (Boolean) ((JavascriptExecutor) driver)
                .executeScript("return localStorage.getItem('com.scopely:scopelyid:idToken') !== null");
    }

    /**
     * Logged in once the header has neither the logged-out icon nor the Login button (which is in the
     * DOM even with the menu closed). Waits, since the header renders logged-out first.
     */
    public boolean isLoggedIn() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10)).until(d ->
                    d.findElements(LOGGED_OUT_ICON).isEmpty() && d.findElements(LOGIN_BUTTON).isEmpty());
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    public HomePage goToShopViaHeader() {
        click(SHOP_LINK);
        return this;
    }
}
