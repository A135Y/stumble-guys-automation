package com.ayusuf.sg.tests;

import com.ayusuf.sg.email.LinkedAccount;
import com.ayusuf.sg.email.MailTmInbox;
import com.ayusuf.sg.flows.EmailSignIn;
import com.ayusuf.sg.flows.GameLinking;
import com.ayusuf.sg.flows.LinkedAccountSetup;
import com.ayusuf.sg.pages.HomePage;
import com.ayusuf.sg.support.Artifacts;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** AUTH-04: new email -> link in the web game -> log in on the website. Creates a real account, saved for AUTH-02/PUR-01. */
@DesktopOnly("links the account in the web game, which mobile browsers don't offer")
@Test(groups = {"e2e-full", "core"})
public class FullAuthFlowTest extends BaseTest {

    public void registerLinkInGameThenLogIn() {
        MailTmInbox inbox = MailTmInbox.create();

        assertThat(new GameLinking(driver, inbox).link()).as("new Scopely account").isTrue();
        assertThat(new EmailSignIn(driver, inbox).signIn()).as("returning user on the website").isFalse();

        HomePage home = new HomePage(driver);
        assertThat(home.hasScopelyToken()).as("Scopely idToken stored").isTrue();
        assertThat(LinkedAccountSetup.isFullyLoggedIn(driver)).as("logged in, no Almost there").isTrue();
        Artifacts.screenshot(driver, "auth04-logged-in");
        LinkedAccount.save(inbox);
    }
}
