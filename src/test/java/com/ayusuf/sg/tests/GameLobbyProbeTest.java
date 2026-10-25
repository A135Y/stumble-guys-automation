package com.ayusuf.sg.tests;

import com.ayusuf.sg.pages.GameButton;
import com.ayusuf.sg.pages.GameCanvas;
import com.ayusuf.sg.support.Artifacts;
import com.ayusuf.sg.support.StepTimer;
import org.openqa.selenium.Keys;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Exploration of lobby buttons (Customize, Events) and how to get back to the lobby.
 * Guest player; downloads 788 MB of assets, so only with -Dgroups=probe.
 */
@Test(groups = "probe")
public class GameLobbyProbeTest extends BaseTest {

    // positions on the lobby capture (880px reference), until each has an image
    private static final double[] CUSTOMIZE = {0.07, 0.43};
    private static final double[] EVENTS = {0.477, 0.89};

    private final List<String> log = new ArrayList<>();

    public void probeCustomizeAndEvents() {
        try {
            GameCanvas game = new GameCanvas(driver).open();
            game.waitFor(GameButton.WELCOME_TITLE, Duration.ofSeconds(60));
            game.click(GameButton.WELCOME_START_PLAYING);
            game.acceptAgeGate(25);
            game.waitFor(GameButton.DOWNLOAD_TITLE, Duration.ofSeconds(30));
            game.click(GameButton.DOWNLOAD_OK);
            game.waitForLobby(Duration.ofMinutes(10));
            canvas(game, "l01-lobby");
            StepTimer.step("lobby");

            visit(game, "customize", CUSTOMIZE);
            visit(game, "events", EVENTS);
        } finally {
            Artifacts.text("summary.txt", String.join("\n", log) + "\n");
            System.out.println("Probe output: " + Artifacts.RUN_DIR.toAbsolutePath());
        }
    }

    /** Opens a lobby screen, captures it, then tries Escape to get back. */
    private void visit(GameCanvas game, String name, double[] at) {
        game.clickAt(at[0], at[1]);
        note("clicked " + name);
        for (int i = 1; i <= 3; i++) {
            sleep(2);
            canvas(game, "l-" + name + "-" + i);
        }
        note(name + ": still in lobby = " + game.isLobbyReady());

        game.holdKey(Keys.ESCAPE, Duration.ofMillis(150));
        sleep(2);
        canvas(game, "l-" + name + "-after-escape");
        boolean back = game.isLobbyReady();
        note(name + ": back in lobby after Escape = " + back);
        if (!back) {
            game.dismissPopups();
            game.holdKey(Keys.ESCAPE, Duration.ofMillis(150));
            sleep(2);
            canvas(game, "l-" + name + "-after-escape-2");
            note(name + ": back in lobby after 2nd Escape = " + game.isLobbyReady());
        }
        StepTimer.step(name + " visited");
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
