package com.ayusuf.sg.flows;

import com.ayusuf.sg.config.Config;
import com.ayusuf.sg.email.EmailInbox;
import com.ayusuf.sg.email.EmailMessage;
import com.ayusuf.sg.email.ScopelyEmailParser;
import com.ayusuf.sg.pages.HomePage;
import com.ayusuf.sg.pages.ScopelySignInPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.time.Instant;

/**
 * Scopely ID sign-in.
 * New email: sign-up link, opened in a second tab (the original tab carries on once approved).
 * Known email: 6-digit code typed into the original tab.
 */
public class EmailSignIn {

    private static final Duration EMAIL_TIMEOUT = Duration.ofSeconds(90);

    private final WebDriver driver;
    private final EmailInbox inbox;

    public EmailSignIn(WebDriver driver, EmailInbox inbox) {
        this.driver = driver;
        this.inbox = inbox;
    }

    /** From the website header. Returns true if this created a new Scopely account. */
    public boolean signIn() {
        ScopelySignInPage page = new HomePage(driver).open().openMenu().clickLogin().continueWithEmail();
        boolean newAccount = complete(page);
        // not urlContains: the Scopely URL carries redirect_uri=https://www.stumbleguys.com in its query
        // then the site swaps ?code= for tokens asynchronously
        HomePage home = new HomePage(driver);
        new WebDriverWait(driver, Duration.ofSeconds(30))
                .until(d -> d.getCurrentUrl().startsWith(Config.baseUrl()) && home.hasScopelyToken());
        return newAccount;
    }

    /** Finishes sign-in on an already open Scopely page (website or game). */
    public boolean complete(ScopelySignInPage page) {
        Instant sent = Instant.now().minusSeconds(5);
        boolean newAccount = page.submitEmail(inbox.address());
        EmailMessage email = inbox.waitForMessage(sent, EmailSignIn::isScopely, EMAIL_TIMEOUT);

        var code = ScopelyEmailParser.oneTimeCode(email.body());
        var link = ScopelyEmailParser.signInLink(email.body());
        if (code.isPresent()) {
            page.enterCode(code.get());
        } else if (link.isPresent()) {
            if (!page.approveWithLink(link.get())) {
                throw new IllegalStateException("Email link did not show the Success page");
            }
        } else {
            throw new IllegalStateException("No code or link in email: " + email.subject());
        }
        return newAccount;
    }

    private static boolean isScopely(EmailMessage m) {
        return m.from().toLowerCase().contains("scopely") || m.body().contains("id.scopely.com");
    }
}
