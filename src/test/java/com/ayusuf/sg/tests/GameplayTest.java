package com.ayusuf.sg.tests;

import com.ayusuf.sg.pages.GameButton;
import com.ayusuf.sg.pages.GameCanvas;
import com.ayusuf.sg.support.Artifacts;
import com.ayusuf.sg.support.ImageMatcher;
import com.ayusuf.sg.support.StepTimer;
import org.openqa.selenium.Keys;
import org.testng.annotations.Test;

import java.awt.image.BufferedImage;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * WGL-05: guest joins a round and the game reacts to the run key (up arrow).
 * Slow and opt-in (-Dgroups=webgl-slow): downloads 788 MB of assets every run and joins a real match.
 */
@Test(groups = "webgl-slow")
public class GameplayTest extends BaseTest {

    private static final Duration HOLD = Duration.ofSeconds(3);

    public void playerRunsWhenArrowIsHeld() throws InterruptedException {
        GameCanvas game = new GameCanvas(driver).open();
        game.waitFor(GameButton.WELCOME_TITLE, Duration.ofSeconds(60));
        game.click(GameButton.WELCOME_START_PLAYING);
        game.acceptAgeGate(25);
        game.waitFor(GameButton.DOWNLOAD_TITLE, Duration.ofSeconds(30));
        game.click(GameButton.DOWNLOAD_OK);
        StepTimer.step("asset download started");

        game.waitForClosingPopups(GameButton.LOBBY_PLAY, Duration.ofMinutes(10));
        Artifacts.image("wgl05-lobby", game.screenshot());
        StepTimer.step("lobby");

        game.click(GameButton.LOBBY_PLAY);
        game.waitForClosingPopups(GameButton.HUD_QUALIFIED, Duration.ofMinutes(2));
        Thread.sleep(3000); // round intro / countdown
        StepTimer.step("in round");

        // the scene moves by itself (obstacles, other players), so compare against standing still
        BufferedImage a = game.screenshot();
        Thread.sleep(HOLD);
        BufferedImage b = game.screenshot();
        double idle = ImageMatcher.meanDifference(a, b);

        game.holdKey(Keys.ARROW_UP, HOLD);
        BufferedImage c = game.screenshot();
        double running = ImageMatcher.meanDifference(b, c);
        Artifacts.image("wgl05-before-run", b);
        Artifacts.image("wgl05-after-run", c);
        StepTimer.step(String.format("idle diff %.1f, running diff %.1f", idle, running));

        assertThat(game.isShown(GameButton.HUD_OBJECTIVE)).as("still in the round").isTrue();
        assertThat(running).as("screen change while running vs standing (idle %.1f)", idle)
                .isGreaterThan(Math.max(8, idle * 2));
    }
}
