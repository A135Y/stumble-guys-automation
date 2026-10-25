package com.ayusuf.sg.pages;

import com.ayusuf.sg.support.ImageMatcher;
import org.testng.annotations.Test;

import java.awt.image.BufferedImage;

import static org.assertj.core.api.Assertions.assertThat;

/** Real canvas captures from the onboarding probe. */
@Test(groups = "unit")
public class GameCanvasTest {

    public void acceptIsGreyAtZeroAndGreenOnceAgeIsSet() {
        assertThat(GameCanvas.isAgeAcceptEnabled(load("age-gate-880"))).as("0 years").isFalse();
        assertThat(GameCanvas.isAgeAcceptEnabled(load("age-gate-25-880"))).as("25 years").isTrue();
    }

    public void downloadPromptRecognisedAndNotConfusedWithOtherScreens() {
        BufferedImage download = load("download-assets-880");

        assertThat(score(download, GameButton.DOWNLOAD_TITLE)).isGreaterThan(0.95);
        assertThat(score(download, GameButton.DOWNLOAD_OK)).isGreaterThan(0.95);
        assertThat(score(download, GameButton.AGE_TITLE)).isLessThan(0.9);
        assertThat(score(load("welcome-880"), GameButton.DOWNLOAD_TITLE)).isLessThan(0.9);
        assertThat(score(load("age-gate-25-880"), GameButton.DOWNLOAD_OK)).isLessThan(0.9);
    }

    public void popupCloseFoundOnlyOnPopups() {
        assertThat(score(load("promo-popup-880"), GameButton.POPUP_CLOSE)).isGreaterThan(0.95);
        for (String other : new String[]{"welcome-880", "welcome-920", "age-gate-880", "age-gate-25-880", "download-assets-880"}) {
            assertThat(score(load(other), GameButton.POPUP_CLOSE)).as(other).isLessThan(0.9);
        }
    }

    public void lobbyPlayRecognised() {
        assertThat(score(load("lobby-880"), GameButton.LOBBY_PLAY)).isGreaterThan(0.95);
        for (String other : new String[]{"welcome-880", "download-assets-880"}) {
            assertThat(score(load(other), GameButton.LOBBY_PLAY)).as(other).isLessThan(0.9);
        }
    }

    public void lobbyNotReadyWhileAPopupCoversIt() {
        // the dimmed PLAY behind the popup still matches (0.97), which is why the popup X is checked too
        assertThat(score(load("promo-popup-880"), GameButton.LOBBY_PLAY)).isGreaterThan(0.9);
        assertThat(GameCanvas.isLobbyReady(load("promo-popup-880"))).isFalse();
        assertThat(GameCanvas.isLobbyReady(load("lobby-880"))).isTrue();
    }

    public void lobbyButtonsAndScreensRecognised() {
        for (String lobby : new String[]{"lobby-880", "lobby-2-880"}) {
            assertThat(score(load(lobby), GameButton.LOBBY_CUSTOMIZE)).as(lobby).isGreaterThan(0.9);
            assertThat(score(load(lobby), GameButton.LOBBY_EVENTS)).as(lobby).isGreaterThan(0.9);
            assertThat(score(load(lobby), GameButton.SCREEN_BACK)).as(lobby).isLessThan(0.9);
        }
        assertThat(score(load("customize-880"), GameButton.CUSTOMIZE_TITLE)).isGreaterThan(0.95);
        assertThat(score(load("events-880"), GameButton.EVENTS_TITLE)).isGreaterThan(0.95);
        assertThat(score(load("customize-880"), GameButton.EVENTS_TITLE)).isLessThan(0.9);
        assertThat(score(load("events-880"), GameButton.CUSTOMIZE_TITLE)).isLessThan(0.9);
        // same back arrow on the screens and their sub-views (drawn slightly differently, hence the lower threshold)
        for (String screen : new String[]{"customize-880", "events-880", "victory-animation-880", "ranked-880"}) {
            assertThat(score(load(screen), GameButton.SCREEN_BACK)).as(screen).isGreaterThan(GameCanvas.BACK_THRESHOLD);
        }
        assertThat(score(load("lobby-2-880"), GameButton.SCREEN_BACK)).isLessThan(GameCanvas.BACK_THRESHOLD);
    }

    public void redPopupCloseAlsoBlocksLobby() {
        assertThat(score(load("offers-popup-880"), GameButton.POPUP_CLOSE_RED)).isGreaterThan(0.95);
        for (String other : new String[]{"lobby-880", "lobby-2-880", "customize-880", "events-880", "welcome-880"}) {
            assertThat(score(load(other), GameButton.POPUP_CLOSE_RED)).as(other).isLessThan(0.9);
        }
        assertThat(GameCanvas.isLobbyReady(load("offers-popup-880"))).isFalse();
        assertThat(GameCanvas.isLobbyReady(load("lobby-2-880"))).isTrue();
    }

    private static double score(BufferedImage canvas, GameButton button) {
        return ImageMatcher.best(canvas, button.image()).score();
    }

    private static BufferedImage load(String name) {
        return ImageMatcher.load("/game/" + name + ".png");
    }
}
