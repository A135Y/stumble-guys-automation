package com.ayusuf.sg.support;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.regex.Pattern;

/** Hard stop before payment (ADR 4). No test may click a payment confirmation or reach an order-complete page. */
public final class PaymentGuard {

    private static final Pattern CONFIRM_TEXT = Pattern.compile(
            "(?i)\\b(pay( now)?|buy now|confirm( purchase| payment)?|place order|complete (purchase|payment)|subscribe)\\b");
    private static final Pattern COMPLETED_URL = Pattern.compile(
            "(?i)(success|thank-?you|receipt|order-?complete|payment-?complete|purchase-?complete)");

    private PaymentGuard() {
    }

    public static boolean isConfirmText(String text) {
        return text != null && CONFIRM_TEXT.matcher(text).find();
    }

    public static boolean isCompletedUrl(String url) {
        return url != null && COMPLETED_URL.matcher(url.replaceAll("[?#].*", "")).find();
    }

    /** Call before clicking anything in a purchase flow. */
    public static void check(WebElement el) {
        String text = el.getText() + " " + el.getDomAttribute("aria-label");
        if (isConfirmText(text)) {
            throw new IllegalStateException("PaymentGuard: refusing to click \"" + el.getText().trim() + "\"");
        }
    }

    public static void assertNotPurchased(WebDriver driver) {
        String current = driver.getWindowHandle();
        for (String handle : driver.getWindowHandles()) {
            driver.switchTo().window(handle);
            String url = driver.getCurrentUrl();
            driver.switchTo().window(current);
            if (isCompletedUrl(url)) {
                throw new IllegalStateException("PaymentGuard: landed on a completed-order page " + url);
            }
        }
    }
}
