package com.ayusuf.sg.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class LoginModal extends BasePage {

    private static final By CONTINUE_WITH_EMAIL = By.xpath(
            "//button[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'continue with email')]");

    public LoginModal(WebDriver driver) {
        super(driver);
    }

    public ScopelySignInPage continueWithEmail() {
        click(CONTINUE_WITH_EMAIL);
        return new ScopelySignInPage(driver).waitUntilLoaded();
    }
}
