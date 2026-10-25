package com.ayusuf.sg.flows;

import com.ayusuf.sg.email.LinkedAccount;
import com.ayusuf.sg.email.MailTmInbox;
import com.ayusuf.sg.pages.AlmostTherePopup;
import com.ayusuf.sg.pages.HomePage;
import org.openqa.selenium.WebDriver;

import java.util.Optional;

/**
 * Precondition for tests that need a logged-in, game-linked account (shop, AUTH-02).
 * Reuses the saved account; if it doesn't end up logged in, links a new one through the web game and saves it.
 */
public final class LinkedAccountSetup {

    private LinkedAccountSetup() {
    }

    public static MailTmInbox signedIn(WebDriver driver) {
        Optional<MailTmInbox> saved = LinkedAccount.load();
        if (saved.isPresent()) {
            new EmailSignIn(driver, saved.get()).signIn();
            if (isFullyLoggedIn(driver)) return saved.get();
            System.out.println("[setup] saved account " + saved.get().address() + " isn't logged in on the site, linking a new one");
        }

        MailTmInbox inbox = MailTmInbox.create();
        new GameLinking(driver, inbox).link();
        new EmailSignIn(driver, inbox).signIn();
        if (!isFullyLoggedIn(driver)) {
            throw new IllegalStateException("Linked " + inbox.address()
                    + " in the game, but the website still isn't logged in after signing in");
        }
        LinkedAccount.save(inbox);
        return inbox;
    }

    /** Instant check: call after EmailSignIn.signIn(), which already waits for the page to settle. */
    public static boolean isFullyLoggedIn(WebDriver driver) {
        return !new AlmostTherePopup(driver).isShownNow() && new HomePage(driver).isLoggedInNow();
    }
}
