package com.ayusuf.sg.pages;

import com.ayusuf.sg.config.Config;
import com.ayusuf.sg.support.PaymentGuard;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

public class ShopPage extends BasePage {

    private static final By PRICE_BUTTONS = By.cssSelector("button[class*='Card_card__price_button']");
    private static final By LOGIN_MODAL_EMAIL = By.xpath(
            "//button[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'continue with email')]");

    public ShopPage(WebDriver driver) {
        super(driver);
    }

    public ShopPage open() {
        driver.get(Config.baseUrl() + "/shop");
        new CookieBanner(driver).denyIfPresent();
        visible(PRICE_BUTTONS);
        return this;
    }

    public List<WebElement> priceButtons() {
        return driver.findElements(PRICE_BUTTONS).stream().filter(WebElement::isDisplayed).toList();
    }

    /** Clicks the price on the n-th visible card and returns the price text, e.g. "£2.99". */
    public String clickPrice(int index) {
        WebElement btn = priceButtons().get(index);
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'})", btn);
        String price = btn.getText().trim();
        PaymentGuard.check(btn);
        click(btn);
        return price;
    }

    public boolean isLoginRequired() {
        return waitUntilVisible(LOGIN_MODAL_EMAIL);
    }
}
