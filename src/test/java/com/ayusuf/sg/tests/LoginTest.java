package com.ayusuf.sg.tests;

import com.ayusuf.sg.config.Config;
import com.ayusuf.sg.driver.DriverManager;
import com.ayusuf.sg.email.LinkedAccount;
import com.ayusuf.sg.email.MailTmInbox;
import com.ayusuf.sg.flows.EmailSignIn;
import com.ayusuf.sg.flows.EmailSignIn.Account;
import com.ayusuf.sg.flows.LinkedAccountSetup;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** AUTH-02: an account already linked to a game profile logs in (code path). */
@Test(groups = {"auth", "core"})
public class LoginTest extends BaseTest {

    /** On desktop, creates the linked account in its own browser if none is saved, so the test doesn't depend on run order. */
    @BeforeClass(groups = {"auth", "core"})
    public void ensureLinkedAccount() {
        if (LinkedAccount.load().isPresent() || Config.platform().isMobile()) return;
        try {
            LinkedAccountSetup.signedIn(DriverManager.start());
        } finally {
            DriverManager.quit();
        }
    }

    public void linkedAccountLogsIn() {
        MailTmInbox inbox = LinkedAccount.load().orElseThrow(() -> new SkipException(
                "No linked account at " + LinkedAccount.file() + "; mobile can't create one (needs the web game). Run AUTH-04 on desktop first."));

        assertThat(new EmailSignIn(driver, inbox).signIn()).as("existing account").isEqualTo(Account.EXISTING);
        assertThat(LinkedAccountSetup.isFullyLoggedIn(driver)).as("logged in, no Almost there").isTrue();
    }
}
