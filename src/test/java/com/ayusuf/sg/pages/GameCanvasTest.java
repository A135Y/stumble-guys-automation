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

    private static double score(BufferedImage canvas, GameButton button) {
        return ImageMatcher.best(canvas, button.image()).score();
    }

    private static BufferedImage load(String name) {
        return ImageMatcher.load("/game/" + name + ".png");
    }
}
