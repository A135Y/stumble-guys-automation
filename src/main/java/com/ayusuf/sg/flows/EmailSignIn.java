package com.ayusuf.sg.flows;

import com.ayusuf.sg.config.Config;
import com.ayusuf.sg.email.EmailInbox;
import com.ayusuf.sg.email.EmailMessage;
import com.ayusuf.sg.email.ScopelyEmailParser;
import com.ayusuf.sg.pages.AlmostTherePopup;
import com.ayusuf.sg.pages.HomePage;
import com.ayusuf.sg.pages.ScopelySignInPage;
import com.ayusuf.sg.support.StepTimer;
import org.openqa.selenium.StaleElementReferenceException;
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

    /** Whether signing in created the Scopely account or used an existing one. */
    public enum Account { NEW, EXISTING }

    private static final Duration EMAIL_TIMEOUT = Duration.ofSeconds(90);

    private final WebDriver driver;
    private final EmailInbox inbox;

    public EmailSignIn(WebDriver driver, EmailInbox inbox) {
        this.driver = driver;
        this.inbox = inbox;
    }

    /** From the website header. */
    public Account signIn() {
        ScopelySignInPage page = new HomePage(driver).open().openMenu().clickLogin().continueWithEmail();
        StepTimer.step("scopely page open");
        Account account = complete(page);
        // not urlContains: the Scopely URL carries redirect_uri=https://www.stumbleguys.com in its query
        // then the site swaps ?code= for tokens, and only after that renders logged in (or "Almost there")
        HomePage home = new HomePage(driver);
        AlmostTherePopup almostThere = new AlmostTherePopup(driver);
        new WebDriverWait(driver, Duration.ofSeconds(30)).ignoring(StaleElementReferenceException.class)
                .until(d -> d.getCurrentUrl().startsWith(Config.baseUrl()) && home.hasScopelyToken()
                        && (home.isLoggedInNow() || almostThere.isShownNow()));
        StepTimer.step("back on site, settled");
        return account;
    }

    /** Finishes sign-in on an already open Scopely page (website or game). */
    public Account complete(ScopelySignInPage page) {
        Instant sent = Instant.now().minusSeconds(5);
        boolean newAccount = page.submitEmail(inbox.address());
        StepTimer.step("email submitted");
        EmailMessage email = inbox.waitForMessage(sent, EmailSignIn::isScopely, EMAIL_TIMEOUT);
        StepTimer.step("email received");

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
        return newAccount ? Account.NEW : Account.EXISTING;
    }

    private static boolean isScopely(EmailMessage m) {
        return m.from().toLowerCase().contains("scopely") || m.body().contains("id.scopely.com");
    }
}
