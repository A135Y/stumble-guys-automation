package com.ayusuf.sg.tests;

import com.ayusuf.sg.pages.GameButton;
import com.ayusuf.sg.pages.GameCanvas;
import com.ayusuf.sg.support.Artifacts;
import com.ayusuf.sg.support.StepTimer;
import org.testng.annotations.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/** WGL-01. No clicks inside the game. */
@DesktopOnly("the web game isn't offered on mobile browsers (/play shows app store links)")
@Test(groups = {"webgl", "core"})
public class GameLoadTest extends BaseTest {

    private static final Duration BUTTON_TIMEOUT = Duration.ofSeconds(10);

    public void gameLoadsToWelcomeScreen() {
        GameCanvas game = new GameCanvas(driver).open();

        game.waitFor(GameButton.WELCOME_TITLE, Duration.ofSeconds(60));
        StepTimer.step("welcome screen matched");
        Artifacts.image("welcome-canvas", game.screenshot());

        // the panel can still be settling when the title first matches ("Logging in..." underneath), so allow a few
        // seconds per button instead of judging one frame
        assertThat(game.waitFor(GameButton.WELCOME_CONTINUE_WITH_EMAIL, BUTTON_TIMEOUT).score()).as("Continue with email").isGreaterThanOrEqualTo(0.9);
        assertThat(game.waitFor(GameButton.WELCOME_START_PLAYING, BUTTON_TIMEOUT).score()).as("Start playing").isGreaterThanOrEqualTo(0.9);
        StepTimer.step("buttons checked");
    }
}
