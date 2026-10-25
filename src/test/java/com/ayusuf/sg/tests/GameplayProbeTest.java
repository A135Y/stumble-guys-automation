package com.ayusuf.sg.tests;

import com.ayusuf.sg.pages.GameButton;
import com.ayusuf.sg.pages.GameCanvas;
import com.ayusuf.sg.support.Artifacts;
import com.ayusuf.sg.support.StepTimer;
import org.openqa.selenium.Keys;
import org.testng.annotations.Test;

import java.awt.image.BufferedImage;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Exploration past onboarding: 788 MB asset download -> lobby -> PLAY! -> round -> keyboard input.
 * Guest player, no account. Slow (downloads every run) and joins a real match, so only with -Dgroups=probe.
 */
@Test(groups = "probe")
public class GameplayProbeTest extends BaseTest {

    // PLAY sits bottom-right of the lobby (seen dimmed behind a promo popup); position until it has an image
    private static final double PLAY_X = 0.886;
    private static final double PLAY_Y = 0.93;

    private final List<String> log = new ArrayList<>();

    public void probeDownloadLobbyAndRound() {
        try {
            GameCanvas game = new GameCanvas(driver).open();
            game.waitFor(GameButton.WELCOME_TITLE, Duration.ofSeconds(60));
            game.click(GameButton.WELCOME_START_PLAYING);
            game.acceptAgeGate(25);
            game.waitFor(GameButton.DOWNLOAD_TITLE, Duration.ofSeconds(30));
            game.click(GameButton.DOWNLOAD_OK);
            StepTimer.step("download started");

            waitUntilStable(game, "p01-download", Duration.ofMinutes(10));
            StepTimer.step("screen stable");
            int closed = game.dismissPopups();
            sleep(5);
            closed += game.dismissPopups();
            note("popups closed: " + closed);
            Artifacts.image("p02-lobby", game.screenshot());
            StepTimer.step("lobby clear");

            game.clickAt(PLAY_X, PLAY_Y);
            note("clicked estimated PLAY! position");
            for (int i = 1; i <= 12; i++) {
                sleep(5);
                int popups = game.dismissPopups();
                if (popups > 0) note("closed " + popups + " popup(s) after Play");
                canvas(game, "p03-after-play-" + i);
            }
            StepTimer.step("watched 60s after Play");

            game.clickAt(0.5, 0.5);
            BufferedImage before = game.screenshot();
            Artifacts.image("p04-before-keys", before);
            game.holdKey("w", Duration.ofSeconds(3));
            BufferedImage afterW = game.screenshot();
            Artifacts.image("p05-after-w", afterW);
            game.holdKey(Keys.SPACE, Duration.ofMillis(300));
            sleep(1);
            BufferedImage afterSpace = game.screenshot();
            Artifacts.image("p06-after-space", afterSpace);
            note(String.format("diff before->W %.1f, W->space %.1f", diff(before, afterW), diff(afterW, afterSpace)));
            StepTimer.step("keys sent");
        } finally {
            Artifacts.text("summary.txt", String.join("\n", log) + "\n");
            System.out.println("Probe output: " + Artifacts.RUN_DIR.toAbsolutePath());
        }
    }

    /** Captures every 10s until three captures in a row barely differ (or timeout). Returns the last one. */
    private BufferedImage waitUntilStable(GameCanvas game, String prefix, Duration timeout) {
        Instant deadline = Instant.now().plus(timeout);
        BufferedImage previous = game.screenshot();
        int still = 0;
        for (int i = 1; Instant.now().isBefore(deadline); i++) {
            sleep(10);
            BufferedImage current = game.screenshot();
            double d = diff(previous, current);
            Artifacts.image(prefix + "-" + i, current);
            note(String.format("%s-%d diff %.1f download prompt=%s", prefix, i, d, game.isShown(GameButton.DOWNLOAD_TITLE)));
            still = d < 3 ? still + 1 : 0;
            if (still >= 3) return current;
            previous = current;
        }
        note("screen never settled within " + timeout.toMinutes() + " min");
        return previous;
    }

    /** Mean absolute grey-level difference (0-255), sampled every 4th pixel. */
    static double diff(BufferedImage a, BufferedImage b) {
        int w = Math.min(a.getWidth(), b.getWidth()), h = Math.min(a.getHeight(), b.getHeight());
        long sum = 0, n = 0;
        for (int y = 0; y < h; y += 4) {
            for (int x = 0; x < w; x += 4) {
                sum += Math.abs(grey(a.getRGB(x, y)) - grey(b.getRGB(x, y)));
                n++;
            }
        }
        return (double) sum / n;
    }

    private static int grey(int rgb) {
        return (((rgb >> 16) & 0xff) * 299 + ((rgb >> 8) & 0xff) * 587 + (rgb & 0xff) * 114) / 1000;
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
