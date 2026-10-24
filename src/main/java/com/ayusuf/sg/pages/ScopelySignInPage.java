package com.ayusuf.sg.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.WindowType;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

/** id.scopely.com. New emails get a sign-up link, known emails get a 6-digit code. */
public class ScopelySignInPage extends BasePage {

    private static final By EMAIL = By.cssSelector("input[name='email']");
    // "Continue" on the email step, "Agree and get sign up link" on the new-account step
    private static final By SUBMIT = By.cssSelector("button[data-test-id='site-email-input-submit-button']");
    private static final By TITLE = By.cssSelector("h1.title");
    // one box per digit; there's also a hidden Okta "credentials.passcode" input to avoid
    private static final By CODE_INPUTS = By.cssSelector("input[class*='OneTimeCodeInput_oneTimeCodeInput']");
    private static final By CODE_SUBMIT = By.cssSelector("button[class*='OneTimeCodeInput_submitButton']");
    private static final By CHECK_INBOX = By.xpath("//*[self::h1 or self::h2][contains(.,'Check your inbox')]");
    private static final By VERIFY_SUCCESS = By.xpath("//h2[contains(.,'Success')]");
    private static final By ERROR = By.cssSelector("[role='alert'], .ant-form-item-explain-error");

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
        // retried: with an existing Scopely session the page can reload and wipe the field mid-submit
        for (int attempt = 1; attempt <= 3; attempt++) {
            if (!typeEmail(email)) continue;
            click(SUBMIT);
            if (!waitForNextStep()) continue;
            if (isNewAccountStep()) {
                click(SUBMIT);
                return true;
            }
            return false;
        }
        throw new IllegalStateException("Email step didn't move on after 3 attempts");
    }

    /** True once a real next step shows; a reload alone (heading briefly missing) doesn't count. */
    private boolean waitForNextStep() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(15)).ignoring(StaleElementReferenceException.class).until(d ->
                    isNewAccountStep() || isDisplayed(CODE_INPUTS) || isDisplayed(CHECK_INBOX));
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    private boolean isDisplayed(By locator) {
        try {
            return driver.findElements(locator).stream().anyMatch(WebElement::isDisplayed);
        } catch (StaleElementReferenceException e) {
            return false;
        }
    }

    private boolean typeEmail(String email) {
        try {
            WebElement field = visible(EMAIL);
            field.clear();
            field.sendKeys(email);
            new WebDriverWait(driver, Duration.ofSeconds(5)).until(d ->
                    email.equals(d.findElement(EMAIL).getDomProperty("value")) && d.findElement(SUBMIT).isEnabled());
            return true;
        } catch (TimeoutException | StaleElementReferenceException e) {
            return false;
        }
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

    public void enterCode(String code) {
        visible(CODE_INPUTS);
        List<WebElement> boxes = driver.findElements(CODE_INPUTS);
        if (boxes.size() != code.length()) {
            throw new IllegalStateException("Expected " + code.length() + " code boxes, found " + boxes.size());
        }
        for (int i = 0; i < code.length(); i++) {
            boxes.get(i).sendKeys(String.valueOf(code.charAt(i)));
        }
        wait.until(ExpectedConditions.elementToBeClickable(CODE_SUBMIT)).click();
    }

    public String errorText() {
        return waitUntilVisible(ERROR) ? driver.findElement(ERROR).getText() : "";
    }
}
