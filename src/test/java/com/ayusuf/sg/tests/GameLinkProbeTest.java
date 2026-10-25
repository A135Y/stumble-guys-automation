package com.ayusuf.sg.tests;

import com.ayusuf.sg.email.MailTmInbox;
import com.ayusuf.sg.flows.EmailSignIn;
import com.ayusuf.sg.pages.GameButton;
import com.ayusuf.sg.pages.GameCanvas;
import com.ayusuf.sg.pages.ScopelySignInPage;
import com.ayusuf.sg.support.Artifacts;
import org.testng.annotations.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Exploration of linking a Scopely account from the web game's welcome screen.
 * Creates/links real accounts, so only runs with -Dgroups=probe.
 * -Dprobe.startPlaying=true also clicks Start Playing afterwards and captures what follows (no further clicks).
 */
@Test(groups = "probe")
public class GameLinkProbeTest extends BaseTest {

    private final List<String> log = new ArrayList<>();

    public void probeLinkFromWelcomeScreen() {
        MailTmInbox inbox = ProbeInbox.get();
        note("inbox: " + inbox.address());

        try {
            GameCanvas game = new GameCanvas(driver).open();
            game.waitFor(GameButton.WELCOME_TITLE, Duration.ofSeconds(60));
            canvas(game, "g01-welcome");

            String gameTab = driver.getWindowHandle();
            Set<String> before = driver.getWindowHandles();
            game.click(GameButton.WELCOME_CONTINUE_WITH_EMAIL);
            note("clicked Continue with email");

            String scopelyTab = waitForScopely(before, Duration.ofSeconds(20));
            if (scopelyTab == null) {
                canvas(game, "g02-no-scopely");
                note("Scopely sign-in did not open (no new tab, no redirect)");
                return;
            }
            note("Scopely opened in " + (scopelyTab.equals(gameTab) ? "same tab" : "new tab/popup"));
            driver.switchTo().window(scopelyTab);
            page("g02-scopely");

            ScopelySignInPage signIn = new ScopelySignInPage(driver).waitUntilLoaded();
            note("new account: " + new EmailSignIn(driver, inbox).complete(signIn));
            sleep(5);
            note("open tabs after sign-in: " + driver.getWindowHandles().size());
            if (driver.getWindowHandles().contains(scopelyTab)) {
                driver.switchTo().window(scopelyTab);
                page("g03-after-signin");
            }

            driver.switchTo().window(gameTab);
            note("game tab url: " + stripQuery(driver.getCurrentUrl()));
            for (int i = 1; i <= 8; i++) {
                sleep(5);
                canvas(game, "g04-after-link-" + i);
            }
            note("welcome still shown: " + game.isShown(GameButton.WELCOME_TITLE));

            if (Boolean.getBoolean("probe.startPlaying") && game.isShown(GameButton.WELCOME_START_PLAYING)) {
                game.click(GameButton.WELCOME_START_PLAYING);
                note("clicked Start Playing");
                for (int i = 1; i <= 6; i++) {
                    sleep(5);
                    canvas(game, "g05-after-start-" + i);
                }
            }
        } finally {
            Artifacts.text("summary.txt", String.join("\n", log) + "\n");
            System.out.println("Probe output: " + Artifacts.RUN_DIR.toAbsolutePath());
        }
    }

    /** Returns the tab showing id.scopely.com, or null. */
    private String waitForScopely(Set<String> before, Duration timeout) {
        String current = driver.getWindowHandle();
        Instant deadline = Instant.now().plus(timeout);
        while (Instant.now().isBefore(deadline)) {
            for (String h : driver.getWindowHandles()) {
                if (!before.contains(h)) return h;
            }
            if (driver.getCurrentUrl().contains("id.scopely.com")) return current;
            sleep(1);
        }
        return null;
    }

    private void canvas(GameCanvas game, String name) {
        try {
            Artifacts.image(name, game.screenshot());
            note(name);
        } catch (RuntimeException e) {
            note(name + ": canvas screenshot failed (" + e.getClass().getSimpleName() + ")");
        }
    }

    private void page(String name) {
        Artifacts.screenshot(driver, name);
        Artifacts.pageSource(driver, name);
        note(name + ": " + stripQuery(driver.getCurrentUrl()));
    }

    private static String stripQuery(String url) {
        return url.replaceAll("[?#].*", "");
    }

    private void note(String line) {
        log.add(line);
        System.out.println("[probe] " + line);
    }

    private static void sleep(int seconds) {
        try {
            Thread.sleep(Duration.ofSeconds(seconds));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
