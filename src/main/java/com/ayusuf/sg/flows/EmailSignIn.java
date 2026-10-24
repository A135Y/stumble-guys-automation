package com.ayusuf.sg.flows;

import com.ayusuf.sg.config.Config;
import com.ayusuf.sg.email.EmailInbox;
import com.ayusuf.sg.email.EmailMessage;
import com.ayusuf.sg.email.ScopelyEmailParser;
import com.ayusuf.sg.pages.HomePage;
import com.ayusuf.sg.pages.ScopelySignInPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.time.Instant;

/**
 * Website sign-in: email -> link and/or code from the inbox -> back on stumbleguys.com.
 * The link is opened in the same driver session, since the PKCE verifier lives in this browser's localStorage.
 */
public class EmailSignIn {

    private static final Duration EMAIL_TIMEOUT = Duration.ofSeconds(90);

    private final WebDriver driver;
    private final EmailInbox inbox;

    public EmailSignIn(WebDriver driver, EmailInbox inbox) {
        this.driver = driver;
        this.inbox = inbox;
    }

    public void signIn() {
        ScopelySignInPage signIn = new HomePage(driver).open().openMenu().clickLogin().continueWithEmail();

        Instant sent = Instant.now().minusSeconds(5);
        signIn.submitEmail(inbox.address());
        EmailMessage email = inbox.waitForMessage(sent, EmailSignIn::isScopely, EMAIL_TIMEOUT);

        var link = ScopelyEmailParser.signInLink(email.body());
        if (link.isPresent() && !signIn.isCodeStepShown()) {
            driver.get(link.get());
        }
        if (signIn.isCodeStepShown()) {
            String code = ScopelyEmailParser.oneTimeCode(email.body())
                    .orElseThrow(() -> new IllegalStateException("No code in email: " + email.subject()));
            signIn.enterCode(code);
        }

        new WebDriverWait(driver, Config.timeout()).until(ExpectedConditions.urlContains("stumbleguys.com"));
    }

    private static boolean isScopely(EmailMessage m) {
        return m.from().toLowerCase().contains("scopely") || m.body().contains("id.scopely.com");
    }
}
