package com.ayusuf.sg.tests;

import com.ayusuf.sg.pages.GameButton;
import com.ayusuf.sg.pages.GameCanvas;
import com.ayusuf.sg.support.Artifacts;
import org.testng.annotations.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Exploration of guest onboarding: welcome -> Start Playing -> age gate -> lobby. No email or account.
 * Accepts the age gate (game ToS) for a guest player, so only runs with -Dgroups=probe.
 */
@Test(groups = "probe")
public class GameOnboardingProbeTest extends BaseTest {

    private final List<String> log = new ArrayList<>();

    public void probeGuestOnboarding() {
        try {
            GameCanvas game = new GameCanvas(driver).open();
            game.waitFor(GameButton.WELCOME_TITLE, Duration.ofSeconds(60));
            canvas(game, "o01-welcome");

            game.click(GameButton.WELCOME_START_PLAYING);
            note("clicked Start Playing");

            if (!waitForAgeGate(game, Duration.ofSeconds(30))) {
                note("no age gate after Start Playing");
                frames(game, "o05-after-start", 8);
                return;
            }
            canvas(game, "o02-age-gate");

            game.clickAgeAccept();
            sleep(3);
            canvas(game, "o03-after-accept-at-0");
            note("age gate still shown after Accept at 0 years: " + game.isShown(GameButton.AGE_TITLE));

            game.setAge(25);
            sleep(1);
            canvas(game, "o04-age-25");

            game.clickAgeAccept();
            note("clicked Accept at 25");
            frames(game, "o05-lobby", 8);
            note("age gate still shown at the end: " + game.isShown(GameButton.AGE_TITLE));
        } finally {
            Artifacts.text("summary.txt", String.join("\n", log) + "\n");
            System.out.println("Probe output: " + Artifacts.RUN_DIR.toAbsolutePath());
        }
    }

    private boolean waitForAgeGate(GameCanvas game, Duration timeout) {
        Instant deadline = Instant.now().plus(timeout);
        for (int i = 1; Instant.now().isBefore(deadline); i++) {
            if (game.isShown(GameButton.AGE_TITLE)) return true;
            canvas(game, "o01b-waiting-" + i);
            sleep(2);
        }
        return false;
    }

    private void frames(GameCanvas game, String prefix, int count) {
        for (int i = 1; i <= count; i++) {
            sleep(3);
            canvas(game, prefix + "-" + i);
        }
    }

    private void canvas(GameCanvas game, String name) {
        Artifacts.image(name, game.screenshot());
        note(name);
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
