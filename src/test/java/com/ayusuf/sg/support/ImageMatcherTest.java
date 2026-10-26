package com.ayusuf.sg.support;

import org.testng.annotations.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/** Real canvas screenshots at two sizes (the ads beside the game resize it). */
@Test(groups = "unit")
public class ImageMatcherTest {

    private static final BufferedImage CONTINUE = ImageMatcher.load("/game/welcome-continue-with-email.png");
    private static final BufferedImage TITLE = ImageMatcher.load("/game/welcome-title.png");

    public void findsButtonAtReferenceSize() {
        BufferedImage canvas = ImageMatcher.load("/game/welcome-880.png");

        var match = ImageMatcher.find(canvas, CONTINUE, 0.9).orElseThrow();

        assertThat(match.x() / canvas.getWidth()).isCloseTo(0.50, within(0.02));
        assertThat(match.y() / canvas.getHeight()).isCloseTo(0.635, within(0.02));
    }

    public void findsButtonOnLargerCanvasOnceScaled() {
        BufferedImage canvas = ImageMatcher.scaleToWidth(ImageMatcher.load("/game/welcome-920.png"), 880);

        var match = ImageMatcher.find(canvas, CONTINUE, 0.9).orElseThrow();

        assertThat(match.y() / canvas.getHeight()).isCloseTo(0.635, within(0.02));
        assertThat(ImageMatcher.find(canvas, TITLE, 0.9)).isPresent();
    }

    public void matchesViewportCropAndOddPixelTemplates() {
        // captured with the viewport-crop screenshot; the title template starts on an odd pixel
        BufferedImage canvas = ImageMatcher.load("/game/welcome-viewport-crop.png");

        assertThat(ImageMatcher.best(canvas, TITLE).score()).isGreaterThan(0.95);
        assertThat(ImageMatcher.best(canvas, CONTINUE).score()).isGreaterThan(0.95);
    }

    public void tellsAgeGateAndWelcomeApart() {
        BufferedImage age = ImageMatcher.load("/game/age-gate-880.png");
        BufferedImage welcome = ImageMatcher.load("/game/welcome-880.png");
        BufferedImage ageTitle = ImageMatcher.load("/game/age-title.png");

        assertThat(ImageMatcher.find(age, ageTitle, 0.9)).isPresent();
        assertThat(ImageMatcher.find(age, ImageMatcher.load("/game/age-accept.png"), 0.9)).isPresent();
        assertThat(ImageMatcher.find(welcome, ageTitle, 0.9)).isEmpty();
        assertThat(ImageMatcher.find(age, TITLE, 0.9)).isEmpty();
        assertThat(ImageMatcher.find(age, CONTINUE, 0.9)).as("scores 0.78, closest false match").isEmpty();
    }

    public void meanDifferenceIsZeroForSameImageAndLargeForDifferentScreens() {
        BufferedImage welcome = ImageMatcher.load("/game/welcome-880.png");
        assertThat(ImageMatcher.meanDifference(welcome, welcome)).isZero();
        assertThat(ImageMatcher.meanDifference(welcome, ImageMatcher.load("/game/age-gate-880.png"))).isGreaterThan(10);
    }

    /** The fast coarse-to-fine search must agree with the exhaustive one on every real capture. */
    public void coarseToFineAgreesWithExhaustiveSearch() throws Exception {
        var dir = Path.of(ImageMatcherTest.class.getResource("/game").toURI());
        var screens = Files.list(dir).filter(f -> f.toString().endsWith("-880.png") || f.toString().endsWith("-920.png")).toList();
        var buttons = Files.list(Path.of(ImageMatcher.class.getResource("/game").toURI())).filter(f -> f.toString().endsWith(".png")).toList();
        assertThat(screens).hasSizeGreaterThan(10);
        assertThat(buttons).hasSizeGreaterThan(10);
        int compared = 0;
        for (var screen : screens) {
            BufferedImage canvas = ImageMatcher.scaleToWidth(ImageIO.read(screen.toFile()), 880);
            for (var button : buttons) {
                BufferedImage img = ImageIO.read(button.toFile());
                var slow = ImageMatcher.bestExhaustive(canvas, img);
                var fast = ImageMatcher.best(canvas, img);
                String what = button.getFileName() + " on " + screen.getFileName();
                assertThat(fast.score()).as(what).isLessThanOrEqualTo(slow.score() + 1e-9);
                if (slow.score() >= 0.85) {
                    assertThat(fast.score()).as(what).isCloseTo(slow.score(), within(0.02));
                    assertThat(Math.abs(fast.x() - slow.x()) + Math.abs(fast.y() - slow.y())).as(what).isLessThanOrEqualTo(4);
                }
                compared++;
            }
        }
        assertThat(compared).isEqualTo(screens.size() * buttons.size());
    }

    public void noMatchWhenButtonIsGone() {
        BufferedImage canvas = copy(ImageMatcher.load("/game/welcome-880.png"));
        Graphics2D g = canvas.createGraphics();
        g.setColor(new Color(40, 90, 230));
        g.fillRect(330, 290, 220, 50);
        g.dispose();

        assertThat(ImageMatcher.find(canvas, CONTINUE, 0.9)).isEmpty();
    }

    private static BufferedImage copy(BufferedImage src) {
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
        out.getGraphics().drawImage(src, 0, 0, null);
        return out;
    }
}
