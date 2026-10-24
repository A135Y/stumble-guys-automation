package com.ayusuf.sg.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.WindowType;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

/** id.scopely.com: email step, new-account step and one-time code step. */
public class ScopelySignInPage extends BasePage {

    private static final By EMAIL = By.cssSelector("input[name='email']");
    // "Continue" on the email step, "Agree and get sign up link" on the new-account step
    private static final By SUBMIT = By.cssSelector("button[data-test-id='site-email-input-submit-button']");
    private static final By TITLE = By.cssSelector("h1.title");
    private static final By CODE_INPUTS = By.cssSelector(
            "input[autocomplete='one-time-code'], input[name*='code' i], input[inputmode='numeric']");
    private static final By VERIFY_SUCCESS = By.xpath("//h2[contains(.,'Success')]");
    private static final By ERROR =By.cssSelector("[role='alert'], .ant-form-item-explain-error");

    public ScopelySignInPage(WebDriver driver) {
        super(driver);
    }

    public ScopelySignInPage waitUntilLoaded() {
        wait.until(ExpectedConditions.urlContains("id.scopely.com"));
        visible(EMAIL);
        return this;
    }

    /** Submits the email and accepts the sign-up step if Scopely doesn't know it. Returns true for a new account. */
    public boolean submitEmail(String email) {
        String before = title();
        WebElement field = visible(EMAIL);
        field.clear();
        field.sendKeys(email);
        click(SUBMIT);

        waitForTitleChange(before);
        if (isNewAccountStep()) {
            click(SUBMIT);
            return true;
        }
        return false;
    }

    public boolean isNewAccountStep() {
        return title().toLowerCase().contains("create a scopely account");
    }

    public String title() {
        return driver.findElements(TITLE).stream().map(WebElement::getText).findFirst().orElse("");
    }

    /**
     * Opens the email link in a new tab, waits for "Success", then returns to this tab.
     * Opening it in this tab would replace the page that's waiting for the approval.
     */
    public boolean approveWithLink(String link) {
        String original = driver.getWindowHandle();
        driver.switchTo().newWindow(WindowType.TAB);
        try {
            driver.get(link);
            return waitUntilVisible(VERIFY_SUCCESS);
        } finally {
            driver.close();
            driver.switchTo().window(original);
        }
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
        if (driver.findElements(SUBMIT).stream().anyMatch(WebElement::isDisplayed)) {
            click(SUBMIT);
        } else {
            inputs.getLast().sendKeys(Keys.ENTER);
        }
    }

    public String errorText() {
        return waitUntilVisible(ERROR) ? driver.findElement(ERROR).getText() : "";
    }

    private void waitForTitleChange(String before) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(d -> !title().equals(before));
        } catch (TimeoutException ignored) {
            // some steps keep the same heading
        }
    }
}
