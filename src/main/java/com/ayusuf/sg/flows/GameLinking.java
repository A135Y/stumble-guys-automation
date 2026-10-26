package com.ayusuf.sg.flows;

import com.ayusuf.sg.email.EmailInbox;
import com.ayusuf.sg.pages.GameButton;
import com.ayusuf.sg.pages.GameCanvas;
import com.ayusuf.sg.pages.ScopelySignInPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.Set;

/** Links a Scopely account to a game profile from the web game's welcome screen. */
public class GameLinking {

    private static final int TEST_AGE = 25;

    private final WebDriver driver;
    private final EmailInbox inbox;

    public GameLinking(WebDriver driver, EmailInbox inbox) {
        this.driver = driver;
        this.inbox = inbox;
    }

    public EmailSignIn.Account link() {
        GameCanvas game = new GameCanvas(driver).open();
        game.waitFor(GameButton.WELCOME_TITLE, Duration.ofSeconds(60));

        String gameTab = driver.getWindowHandle();
        Set<String> before = driver.getWindowHandles();
        game.click(GameButton.WELCOME_CONTINUE_WITH_EMAIL);

        // Scopely sign-in opens in a popup that closes itself when done
        String popup = new WebDriverWait(driver, Duration.ofSeconds(20)).until(d ->
                d.getWindowHandles().stream().filter(h -> !before.contains(h)).findFirst().orElse(null));
        driver.switchTo().window(popup);
        EmailSignIn.Account account = new EmailSignIn(driver, inbox).complete(new ScopelySignInPage(driver).waitUntilLoaded());

        new WebDriverWait(driver, Duration.ofSeconds(30)).until(d -> !d.getWindowHandles().contains(popup));
        driver.switchTo().window(gameTab);

        game.acceptAgeGate(TEST_AGE);
        return account;
    }
}
