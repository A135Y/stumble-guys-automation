package com.ayusuf.sg.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import java.time.Duration;

/** Shown when the Scopely account isn't linked to a game profile yet. */
public class AlmostTherePopup extends BasePage {

    private static final By POPUP = By.xpath(
            "//*[contains(translate(normalize-space(text()),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'almost there')]"
                    + " | //img[contains(@src,'link_your_game')]");

    public AlmostTherePopup(WebDriver driver) {
        super(driver);
    }

    public boolean isShown() {
        return waitUntilVisible(POPUP);
    }

    public boolean isShownNow() {
        return driver.findElements(POPUP).stream().anyMatch(e -> e.isDisplayed());
    }

    public boolean isShown(Duration within) {
        return waitUntilVisible(POPUP, within);
    }
}
