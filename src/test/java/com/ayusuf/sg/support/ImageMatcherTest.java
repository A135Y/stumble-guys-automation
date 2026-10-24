package com.ayusuf.sg.support;

import org.testng.annotations.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/** Real canvas screenshots at two sizes (the ads beside the game resize it). */
@Test(groups = "unit")
public class ImageMatcherTest {

    private static final BufferedImage CONTINUE = ImageMatcher.load("/game/welcome-continue-with-email.png");
    private static final BufferedImage TITLE = ImageMatcher.load("/game/welcome-title.png");

    public void findsButtonAtReferenceSize() {
        BufferedImage canvas = ImageMatcher.load("/game/welcome-880.png");

        var match = ImageMatcher.find(canvas, CONTINUE, 0.8).orElseThrow();

        assertThat(match.x() / canvas.getWidth()).isCloseTo(0.50, within(0.02));
        assertThat(match.y() / canvas.getHeight()).isCloseTo(0.635, within(0.02));
    }

    public void findsButtonOnLargerCanvasOnceScaled() {
        BufferedImage canvas = ImageMatcher.scaleToWidth(ImageMatcher.load("/game/welcome-920.png"), 880);

        var match = ImageMatcher.find(canvas, CONTINUE, 0.8).orElseThrow();

        assertThat(match.y() / canvas.getHeight()).isCloseTo(0.635, within(0.02));
        assertThat(ImageMatcher.find(canvas, TITLE, 0.8)).isPresent();
    }

    public void tellsAgeGateAndWelcomeApart() {
        BufferedImage age = ImageMatcher.load("/game/age-gate-880.png");
        BufferedImage welcome = ImageMatcher.load("/game/welcome-880.png");
        BufferedImage ageTitle = ImageMatcher.load("/game/age-title.png");

        assertThat(ImageMatcher.find(age, ageTitle, 0.8)).isPresent();
        assertThat(ImageMatcher.find(age, ImageMatcher.load("/game/age-accept.png"), 0.8)).isPresent();
        assertThat(ImageMatcher.find(welcome, ageTitle, 0.8)).isEmpty();
        assertThat(ImageMatcher.find(age, TITLE, 0.8)).isEmpty();
    }

    public void noMatchWhenButtonIsGone() {
        BufferedImage canvas = copy(ImageMatcher.load("/game/welcome-880.png"));
        Graphics2D g = canvas.createGraphics();
        g.setColor(new Color(40, 90, 230));
        g.fillRect(330, 290, 220, 50);
        g.dispose();

        assertThat(ImageMatcher.find(canvas, CONTINUE, 0.8)).isEmpty();
    }

    private static BufferedImage copy(BufferedImage src) {
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
        out.getGraphics().drawImage(src, 0, 0, null);
        return out;
    }
}
