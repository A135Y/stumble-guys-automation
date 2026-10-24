package com.ayusuf.sg.pages;

import com.ayusuf.sg.config.Config;
import com.ayusuf.sg.support.ImageMatcher;
import com.ayusuf.sg.support.ImageMatcher.Match;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.WebDriverWait;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * The Unity WebGL game at /play. Its UI is drawn on a canvas, so buttons are found by image matching
 * and clicked relative to the canvas, which changes size as the ads around it change.
 */
public class GameCanvas extends BasePage {

    /** Templates are cut from canvas screenshots scaled to this width. */
    public static final int REF_WIDTH = 880;
    private static final double MATCH_THRESHOLD = 0.8;
    private static final Duration LOAD_TIMEOUT = Duration.ofSeconds(90);

    private static final By CANVAS = By.cssSelector("canvas#react-unity-webgl-canvas-1");
    private static final By LOADING_PERCENT = By.xpath("//*[contains(@class,'font-lilita') and contains(normalize-space(.),'%')]");

    public GameCanvas(WebDriver driver) {
        super(driver);
    }

    public GameCanvas open() {
        driver.get(Config.baseUrl() + "/play");
        new CookieBanner(driver).denyIfPresent();
        return waitUntilLoaded();
    }

    public GameCanvas waitUntilLoaded() {
        new WebDriverWait(driver, LOAD_TIMEOUT).until(d -> {
            WebElement c = d.findElements(CANVAS).stream().findFirst().orElse(null);
            return c != null && c.getSize().getWidth() > 300 && d.findElements(LOADING_PERCENT).isEmpty();
        });
        return this;
    }

    public BufferedImage screenshot() {
        byte[] png = canvas().getScreenshotAs(OutputType.BYTES);
        try {
            return ImageMatcher.scaleToWidth(ImageIO.read(new ByteArrayInputStream(png)), REF_WIDTH);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public Optional<Match> find(GameButton button) {
        return ImageMatcher.find(screenshot(), button.image(), MATCH_THRESHOLD);
    }

    public boolean isShown(GameButton button) {
        return find(button).isPresent();
    }

    public Match waitFor(GameButton button, Duration timeout) {
        Instant deadline = Instant.now().plus(timeout);
        do {
            Optional<Match> m = find(button);
            if (m.isPresent()) return m.get();
            sleep(Duration.ofSeconds(1));
        } while (Instant.now().isBefore(deadline));
        throw new IllegalStateException(button + " not shown within " + timeout.toSeconds() + "s");
    }

    public void click(GameButton button) {
        BufferedImage shot = screenshot();
        Match m = ImageMatcher.find(shot, button.image(), MATCH_THRESHOLD)
                .orElseThrow(() -> new IllegalStateException(button + " not on screen"));
        clickAt(m.x() / shot.getWidth(), m.y() / shot.getHeight());
    }

    /** Clicks at a fraction of the canvas size, (0,0) top-left to (1,1) bottom-right. */
    public void clickAt(double fx, double fy) {
        WebElement c = canvas();
        int dx = (int) Math.round((fx - 0.5) * c.getSize().getWidth());
        int dy = (int) Math.round((fy - 0.5) * c.getSize().getHeight());
        new Actions(driver).moveToElement(c, dx, dy).click().perform();
    }

    public void dragAt(double fromX, double fromY, double toX, double toY) {
        WebElement c = canvas();
        int w = c.getSize().getWidth(), h = c.getSize().getHeight();
        new Actions(driver)
                .moveToElement(c, (int) ((fromX - 0.5) * w), (int) ((fromY - 0.5) * h))
                .clickAndHold()
                .moveToElement(c, (int) ((toX - 0.5) * w), (int) ((toY - 0.5) * h))
                .release()
                .perform();
    }

    private WebElement canvas() {
        return visible(CANVAS);
    }

    private static void sleep(Duration d) {
        try {
            Thread.sleep(d);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
