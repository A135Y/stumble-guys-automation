package com.ayusuf.sg.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.time.Duration;
import java.util.List;

/** id.scopely.com: email step and one-time code step. */
public class ScopelySignInPage extends BasePage {

    private static final By EMAIL = By.cssSelector("input[placeholder='your.email@example.com'], input[type='email'], input[name='identifier']");
    private static final By CONTINUE = By.xpath("//button[normalize-space(.)='Continue']");
    private static final By CODE_INPUTS = By.cssSelector(
            "input[autocomplete='one-time-code'], input[name*='code' i], input[inputmode='numeric']");
    private static final By ERROR = By.cssSelector("[role='alert'], .o-form-error-container, .okta-form-infobox-error");

    public ScopelySignInPage(WebDriver driver) {
        super(driver);
    }

    public ScopelySignInPage waitUntilLoaded() {
        wait.until(ExpectedConditions.urlContains("id.scopely.com"));
        visible(EMAIL);
        return this;
    }

    public ScopelySignInPage submitEmail(String email) {
        WebElement field = visible(EMAIL);
        field.clear();
        field.sendKeys(email);
        click(CONTINUE);
        return this;
    }

    public boolean isCodeStepShown() {
        return waitUntilVisible(CODE_INPUTS, Duration.ofSeconds(5));
    }

    /** Handles both a single input and one box per digit. */
    public void enterCode(String code) {
        visible(CODE_INPUTS);
        List<WebElement> inputs = driver.findElements(CODE_INPUTS).stream().filter(WebElement::isDisplayed).toList();
        if (inputs.size() >= code.length()) {
            for (int i = 0; i < code.length(); i++) inputs.get(i).sendKeys(String.valueOf(code.charAt(i)));
        } else {
            inputs.getFirst().sendKeys(code);
        }
        if (driver.findElements(CONTINUE).stream().anyMatch(WebElement::isDisplayed)) {
            click(CONTINUE);
        } else {
            inputs.getLast().sendKeys(Keys.ENTER);
        }
    }

    public String errorText() {
        return waitUntilVisible(ERROR) ? driver.findElement(ERROR).getText() : "";
    }
}
