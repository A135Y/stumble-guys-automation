package com.ayusuf.sg.pages;

import com.ayusuf.sg.support.Artifacts;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchFrameException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

/**
 * Xsolla checkout opened by a shop price button, inside iframe.payment-container-iframe.
 * Read-only on purpose: nothing here fills payment details or clicks Pay (ADR 4).
 */
public class CheckoutOverlay extends BasePage {

    private static final By FRAME = By.cssSelector("iframe.payment-container-iframe");
    // desktop: a "Pay" button (card form). Mobile leads with Google Pay: a psdk-google-pay-button element wrapping
    // Google's frame (the "Pay"-labelled button next to it is display:none). psdk-apple-pay is the iOS equivalent.
    private static final By PAY_BUTTON = By.xpath("//button[normalize-space(.)='Pay'] | //psdk-google-pay-button | //psdk-apple-pay");
    private static final By CLOSE_BUTTON = By.cssSelector("button[aria-label*='lose' i], button[data-testid*='close' i]");

    public CheckoutOverlay(WebDriver driver) {
        super(driver);
    }

    public CheckoutOverlay waitUntilShown() {
        visible(FRAME);
        try {
            new WebDriverWait(driver, Duration.ofSeconds(30)).until(d -> inForm(() -> isDisplayed(PAY_BUTTON)));
        } catch (RuntimeException e) {
            inFrame(() -> Artifacts.pageSource(driver, "checkout-frame"));
            throw new IllegalStateException("Checkout opened but no Pay button found; frame HTML saved", e);
        }
        return this;
    }

    public boolean isShown() {
        return driver.findElements(FRAME).stream().anyMatch(WebElement::isDisplayed);
    }

    /** All visible text of the payment form (summary, total, payment types). */
    public String text() {
        return inForm(() -> driver.findElement(By.tagName("body")).getText());
    }

    public boolean hasPayButton() {
        return inForm(() -> isDisplayed(PAY_BUTTON));
    }

    /** Closes the overlay with its X if one can be found, otherwise Escape. */
    public void close() {
        boolean clicked = inForm(() -> {
            List<WebElement> close = driver.findElements(CLOSE_BUTTON).stream().filter(WebElement::isDisplayed).toList();
            if (close.isEmpty()) return false;
            close.getFirst().click();
            return true;
        });
        if (!clicked) new Actions(driver).sendKeys(Keys.ESCAPE).perform();
        new WebDriverWait(driver, Duration.ofSeconds(10)).until(d -> !isShown());
    }

    /** Runs inside the frame that holds the Pay button (the payment frame or one nested inside it). */
    private <T> T inForm(Supplier<T> action) {
        return inFrame(() -> {
            if (isDisplayed(PAY_BUTTON)) return action.get();
            for (WebElement nested : driver.findElements(By.tagName("iframe"))) {
                try {
                    driver.switchTo().frame(nested);
                    if (isDisplayed(PAY_BUTTON)) return action.get();
                } catch (NoSuchFrameException ignored) {
                    // frame went away
                }
                driver.switchTo().parentFrame();
            }
            return action.get();
        });
    }

    private <T> T inFrame(Supplier<T> action) {
        driver.switchTo().frame(driver.findElement(FRAME));
        try {
            return action.get();
        } finally {
            driver.switchTo().defaultContent();
        }
    }

    private boolean isDisplayed(By locator) {
        return driver.findElements(locator).stream().anyMatch(WebElement::isDisplayed);
    }
}
