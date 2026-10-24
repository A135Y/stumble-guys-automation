package com.ayusuf.sg.tests;

import com.ayusuf.sg.email.MailTmInbox;
import com.ayusuf.sg.flows.EmailSignIn;
import com.ayusuf.sg.pages.AlmostTherePopup;
import com.ayusuf.sg.pages.HomePage;
import com.ayusuf.sg.support.Artifacts;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** AUTH-01. Creates a real Scopely account on every run. */
@Test(groups = "auth")
public class RegisterTest extends BaseTest {

    public void newEmailSignsInButStopsAtAlmostThere() {
        MailTmInbox inbox = MailTmInbox.create();
        Artifacts.text("register-inbox.properties", "address=" + inbox.address() + "\npassword=" + inbox.password() + "\n");

        new EmailSignIn(driver, inbox).signIn();

        HomePage home = new HomePage(driver);
        assertThat(driver.getCurrentUrl()).startsWith("https://www.stumbleguys.com");
        assertThat(home.hasScopelyToken()).as("Scopely idToken stored").isTrue();
        assertThat(new AlmostTherePopup(driver).isShown()).as("Almost there popup").isTrue();
        assertThat(home.isLoggedOut()).as("site still logged out until linked").isTrue();
    }
}
