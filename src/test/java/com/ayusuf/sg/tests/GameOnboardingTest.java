package com.ayusuf.sg.tests;

import com.ayusuf.sg.pages.GameButton;
import com.ayusuf.sg.pages.GameCanvas;
import com.ayusuf.sg.support.Artifacts;
import com.ayusuf.sg.support.StepTimer;
import org.testng.annotations.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * In-game tests on a guest player (no account). Accepting the age gate accepts the game's ToS for that guest.
 * WGL-04: Accept is blocked until an age is set. WGL-02: onboarding gets past the age gate to asset loading.
 */
@Test(groups = "webgl")
public class GameOnboardingTest extends BaseTest {

    private GameCanvas startAsGuest() {
        GameCanvas game = new GameCanvas(driver).open();
        game.waitFor(GameButton.WELCOME_TITLE, Duration.ofSeconds(60));
        game.click(GameButton.WELCOME_START_PLAYING);
        game.waitFor(GameButton.AGE_TITLE, Duration.ofSeconds(30));
        StepTimer.step("age gate shown");
        return game;
    }

    public void ageGateBlocksAcceptUntilAgeIsSet() {
        GameCanvas game = startAsGuest();
        assertThat(game.isAgeAcceptEnabled()).as("Accept enabled at 0 years").isFalse();

        game.clickAgeAccept();
        assertThat(game.isShown(GameButton.AGE_TITLE)).as("still on age gate after Accept at 0").isTrue();
        StepTimer.step("accept at 0 blocked");

        game.setAge(25);
        assertThat(game.isAgeAcceptEnabled()).as("Accept enabled at 25").isTrue();
        Artifacts.image("wgl04-age-25", game.screenshot());
        StepTimer.step("accept enabled at 25");
    }

    public void guestOnboardingReachesAssetDownload() {
        GameCanvas game = startAsGuest();

        game.acceptAgeGate(25);
        StepTimer.step("age gate accepted");

        game.waitFor(GameButton.DOWNLOAD_TITLE, Duration.ofSeconds(30));
        assertThat(game.isShown(GameButton.DOWNLOAD_OK)).as("Download Assets OK button").isTrue();
        Artifacts.image("wgl02-download-prompt", game.screenshot());
        StepTimer.step("download prompt shown");
    }
}
