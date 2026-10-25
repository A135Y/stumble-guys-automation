package com.ayusuf.sg.tests;

import com.ayusuf.sg.pages.GameButton;
import com.ayusuf.sg.pages.GameCanvas;
import com.ayusuf.sg.support.Artifacts;
import org.testng.annotations.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/** WGL-01. No clicks inside the game. */
@Test(groups = "webgl")
public class GameLoadTest extends BaseTest {

    public void gameLoadsToWelcomeScreen() {
        GameCanvas game = new GameCanvas(driver).open();

        game.waitFor(GameButton.WELCOME_TITLE, Duration.ofSeconds(60));
        Artifacts.image("welcome-canvas", game.screenshot());

        assertThat(game.isShown(GameButton.WELCOME_CONTINUE_WITH_EMAIL)).as("Continue with email").isTrue();
        assertThat(game.isShown(GameButton.WELCOME_START_PLAYING)).as("Start playing").isTrue();
    }
}
