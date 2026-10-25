package com.ayusuf.sg.tests;

import com.ayusuf.sg.pages.GameButton;
import com.ayusuf.sg.pages.GameCanvas;
import com.ayusuf.sg.support.Artifacts;
import com.ayusuf.sg.support.StepTimer;
import org.testng.annotations.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * WGL-05 / WGL-06: lobby screens open, a tab inside them responds, and back returns to the lobby. Guest player.
 * Slow and opt-in (-Dgroups=webgl-slow): the lobby sits behind a 788 MB download, done every run.
 */
@Test(groups = "webgl-slow")
public class GameLobbyTest extends BaseTest {

    // positions on the 880px reference canvas, measured from the lobby probe captures
    private static final double[] CUSTOMIZE_SECOND_TAB = {0.074, 0.384};
    private static final double[] EVENTS_RANKED_TAB = {0.50, 0.90};
    private static final Duration SETTLE = Duration.ofSeconds(2);

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

        double change = game.clickAndMeasureChange(CUSTOMIZE_SECOND_TAB[0], CUSTOMIZE_SECOND_TAB[1], SETTLE);
        Artifacts.image("wgl05-tab-before", game.lastChange()[0]);
        Artifacts.image("wgl05-tab-after", game.lastChange()[1]);
        StepTimer.step(String.format("second tab clicked, change x%.1f vs idle", change));
        assertThat(change).as("customize tab changes the screen more than idle animation").isGreaterThan(3);

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

        double change = game.clickAndMeasureChange(EVENTS_RANKED_TAB[0], EVENTS_RANKED_TAB[1], SETTLE);
        Artifacts.image("wgl06-ranked-before", game.lastChange()[0]);
        Artifacts.image("wgl06-ranked-after", game.lastChange()[1]);
        StepTimer.step(String.format("ranked tab clicked, change x%.1f vs idle", change));
        assertThat(change).as("Ranked tab changes the screen more than idle animation").isGreaterThan(3);

        game.backToLobby();
        assertThat(game.isLobbyReady()).as("back in lobby").isTrue();
        StepTimer.step("back in lobby");
    }
}
