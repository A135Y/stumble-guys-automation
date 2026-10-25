package com.ayusuf.sg.pages;

import com.ayusuf.sg.config.Config;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public abstract class BasePage {

    protected final WebDriver driver;
    protected final WebDriverWait wait;

    protected BasePage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Config.timeout());
    }

    protected WebElement visible(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    protected void click(By locator) {
        click(wait.until(ExpectedConditions.elementToBeClickable(locator)));
    }

    protected void click(WebElement el) {
        wait.until(ExpectedConditions.elementToBeClickable(el));
        try {
            el.click();
        } catch (ElementClickInterceptedException e) {
            // ads/overlays sometimes sit on top of the target
            ((JavascriptExecutor) driver).executeScript("arguments[0].click()", el);
        }
    }

    protected boolean waitUntilVisible(By locator) {
        return waitUntilVisible(locator, Config.timeout());
    }

    protected boolean waitUntilVisible(By locator, Duration timeout) {
        try {
            new WebDriverWait(driver, timeout).until(ExpectedConditions.visibilityOfElementLocated(locator));
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }
}
