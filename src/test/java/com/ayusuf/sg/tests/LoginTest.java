package com.ayusuf.sg.tests;

import com.ayusuf.sg.email.LinkedAccount;
import com.ayusuf.sg.email.MailTmInbox;
import com.ayusuf.sg.flows.EmailSignIn;
import com.ayusuf.sg.pages.AlmostTherePopup;
import com.ayusuf.sg.pages.HomePage;
import org.testng.SkipException;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** AUTH-02: an account already linked to a game profile logs in (code path). */
@Test(groups = "auth")
public class LoginTest extends BaseTest {

    public void linkedAccountLogsIn() {
        MailTmInbox inbox = LinkedAccount.load().orElseThrow(() -> new SkipException(
                "No linked account at " + LinkedAccount.file() + ". Run AUTH-04 first: ./mvnw test -Dgroups=e2e-full"));

        assertThat(new EmailSignIn(driver, inbox).signIn()).as("existing account").isFalse();

        HomePage home = new HomePage(driver);
        assertThat(new AlmostTherePopup(driver).isShown()).as("Almost there popup").isFalse();
        assertThat(home.isLoggedIn()).as("header shows logged in").isTrue();
    }
}
