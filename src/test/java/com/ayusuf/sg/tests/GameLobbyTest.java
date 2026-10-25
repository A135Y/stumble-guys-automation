package com.ayusuf.sg.tests;

import com.ayusuf.sg.pages.GameButton;
import com.ayusuf.sg.pages.GameCanvas;
import com.ayusuf.sg.support.Artifacts;
import com.ayusuf.sg.support.StepTimer;
import org.testng.annotations.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * WGL-05 / WGL-06: lobby screens open and the back arrow returns to the lobby. Guest player.
 * Slow and opt-in (-Dgroups=webgl-slow): the lobby sits behind a 788 MB download, done every run.
 */
@Test(groups = "webgl-slow")
public class GameLobbyTest extends BaseTest {

    private GameCanvas reachLobby() {
        GameCanvas game = new GameCanvas(driver).open();
        game.waitFor(GameButton.WELCOME_TITLE, Duration.ofSeconds(60));
        game.click(GameButton.WELCOME_START_PLAYING);
        game.acceptAgeGate(25);
        game.waitFor(GameButton.DOWNLOAD_TITLE, Duration.ofSeconds(30));
        game.click(GameButton.DOWNLOAD_OK);
        game.waitForLobby(Duration.ofMinutes(10));
        StepTimer.step("lobby");
        return game;
    }

    public void customizeOpensAndBackReturnsToLobby() {
        GameCanvas game = reachLobby();

        game.openFromLobby(GameButton.LOBBY_CUSTOMIZE, GameButton.CUSTOMIZE_TITLE);
        Artifacts.image("wgl05-customize", game.screenshot());
        assertThat(game.isLobbyReady()).as("left the lobby").isFalse();
        StepTimer.step("customize open");

        game.backToLobby();
        assertThat(game.isLobbyReady()).as("back in lobby").isTrue();
        StepTimer.step("back in lobby");
    }

    public void eventsOpensAndBackReturnsToLobby() {
        GameCanvas game = reachLobby();

        game.openFromLobby(GameButton.LOBBY_EVENTS, GameButton.EVENTS_TITLE);
        Artifacts.image("wgl06-events", game.screenshot());
        assertThat(game.isLobbyReady()).as("left the lobby").isFalse();
        StepTimer.step("events open");

        game.backToLobby();
        assertThat(game.isLobbyReady()).as("back in lobby").isTrue();
        StepTimer.step("back in lobby");
    }
}
