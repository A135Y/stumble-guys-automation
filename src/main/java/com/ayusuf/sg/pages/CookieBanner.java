package com.ayusuf.sg.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/** Usercentrics banner, rendered in a shadow root. */
public class CookieBanner extends BasePage {

    private static final By HOST = By.cssSelector("aside#usercentrics-cmp-ui");
    private static final By DENY = By.cssSelector("#deny");

    public CookieBanner(WebDriver driver) {
        super(driver);
    }

    /** Clicks Deny if the banner shows up within a few seconds. */
    public void denyIfPresent() {
        try {
            WebElement deny = new WebDriverWait(driver, Duration.ofSeconds(8))
                    .ignoring(NoSuchElementException.class)
                    .ignoring(StaleElementReferenceException.class)
                    .until(d -> {
                        SearchContext root = d.findElement(HOST).getShadowRoot();
                        WebElement btn = root.findElement(DENY);
                        return btn.isDisplayed() ? btn : null;
                    });
            deny.click();
            wait.until(d -> d.findElements(HOST).isEmpty() || !isBannerShown());
        } catch (TimeoutException ignored) {
            // no banner (consent already stored)
        }
    }

    private boolean isBannerShown() {
        try {
            return driver.findElement(HOST).getShadowRoot().findElement(DENY).isDisplayed();
        } catch (NoSuchElementException | StaleElementReferenceException e) {
            return false;
        }
    }
}
