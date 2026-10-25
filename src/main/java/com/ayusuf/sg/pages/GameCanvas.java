package com.ayusuf.sg.pages;

import com.ayusuf.sg.config.Config;
import com.ayusuf.sg.support.Artifacts;
import com.ayusuf.sg.support.CursorOverlay;
import com.ayusuf.sg.support.ImageMatcher;
import com.ayusuf.sg.support.StepTimer;
import com.ayusuf.sg.support.ImageMatcher.Match;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * The Unity WebGL game at /play. Its UI is drawn on a canvas, so buttons are found by image matching
 * and clicked relative to the canvas, which changes size as the ads around it change.
 */
public class GameCanvas extends BasePage {

    /** Templates are cut from canvas screenshots scaled to this width. */
    public static final int REF_WIDTH = 880;
    private static final double MATCH_THRESHOLD = 0.9;
    // loads took 22-90s+; keep waiting while the percentage moves, give up after 45s without change (once past 0 %)
    private static final Duration LOAD_TIMEOUT = Duration.ofMinutes(3);
    private static final Duration STALL_TIMEOUT = Duration.ofSeconds(45);
    private static final int LOAD_ATTEMPTS = 2;

    private static final By CANVAS = By.cssSelector("canvas#react-unity-webgl-canvas-1");
    private static final By LOADING_PERCENT = By.xpath("//*[contains(@class,'font-lilita') and contains(normalize-space(.),'%')]");

    public GameCanvas(WebDriver driver) {
        super(driver);
    }

    /** Loads /play; if the game's loader stops moving (seen stuck at 75%), reloads once. */
    public GameCanvas open() {
        String result = "";
        for (int attempt = 1; attempt <= LOAD_ATTEMPTS; attempt++) {
            driver.get(Config.baseUrl() + "/play");
            StepTimer.step("play page loaded");
            new CookieBanner(driver).denyIfPresent();
            StepTimer.step("cookie banner handled");
            result = waitUntilLoaded();
            if (result.isEmpty()) {
                StepTimer.step("game loaded");
                return this;
            }
            StepTimer.step("game load " + result + ", reloading");
        }
        throw new IllegalStateException("Game didn't load after " + LOAD_ATTEMPTS + " attempts (" + result + ")");
    }

    /** Empty once the canvas is up and the loader is gone, otherwise why it gave up ("stalled at 75 %"). */
    public String waitUntilLoaded() {
        Instant deadline = Instant.now().plus(LOAD_TIMEOUT);
        String last = null;
        Instant lastChange = Instant.now();
        while (Instant.now().isBefore(deadline)) {
            WebElement c = driver.findElements(CANVAS).stream().findFirst().orElse(null);
            String progress = loaderText();
            if (c != null && c.getSize().getWidth() > 300 && progress.isEmpty()) return "";
            // 0 % covers the main file download, which on a slow connection takes well over 45s
            boolean started = !progress.isEmpty() && !progress.matches("0\\s*%");
            if (!progress.equals(last)) {
                last = progress;
                lastChange = Instant.now();
            } else if (started && Duration.between(lastChange, Instant.now()).compareTo(STALL_TIMEOUT) > 0) {
                return "stalled at " + progress + " for " + STALL_TIMEOUT.toSeconds() + "s";
            }
            sleep(Duration.ofSeconds(1));
        }
        return "still loading (" + loaderText() + ") after " + LOAD_TIMEOUT.toMinutes() + " min";
    }

    private String loaderText() {
        return driver.findElements(LOADING_PERCENT).stream().map(e -> e.getText().trim()).findFirst().orElse("");
    }

    /**
     * Viewport screenshot cropped to the canvas. Element screenshots make Chrome resize the viewport
     * for a moment, which re-lays out the page and the game (visible flicker, missed matches).
     */
    public BufferedImage screenshot() {
        @SuppressWarnings("unchecked")
        Map<String, Number> r = (Map<String, Number>) ((JavascriptExecutor) driver).executeScript(
                "const b = arguments[0].getBoundingClientRect();"
                        + "return {x: b.x, y: b.y, w: b.width, h: b.height, dpr: window.devicePixelRatio};", canvas());
        CursorOverlay.setVisible(driver, false);
        try {
            BufferedImage page = ImageIO.read(new ByteArrayInputStream(
                    ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES)));
            double dpr = r.get("dpr").doubleValue();
            int x = clamp((int) Math.round(r.get("x").doubleValue() * dpr), page.getWidth() - 1);
            int y = clamp((int) Math.round(r.get("y").doubleValue() * dpr), page.getHeight() - 1);
            int w = Math.min((int) Math.round(r.get("w").doubleValue() * dpr), page.getWidth() - x);
            int h = Math.min((int) Math.round(r.get("h").doubleValue() * dpr), page.getHeight() - y);
            return ImageMatcher.scaleToWidth(page.getSubimage(x, y, w, h), REF_WIDTH);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            CursorOverlay.setVisible(driver, true);
        }
    }

    private static int clamp(int v, int max) {
        return Math.max(0, Math.min(v, max));
    }

    public Optional<Match> find(GameButton button) {
        return ImageMatcher.find(screenshot(), button.image(), MATCH_THRESHOLD);
    }

    public boolean isShown(GameButton button) {
        return find(button).isPresent();
    }

    public Match waitFor(GameButton button, Duration timeout) {
        Instant deadline = Instant.now().plus(timeout);
        BufferedImage shot;
        Match best;
        do {
            shot = screenshot();
            best = ImageMatcher.best(shot, button.image());
            if (best.score() >= MATCH_THRESHOLD) return best;
            sleep(Duration.ofSeconds(1));
        } while (Instant.now().isBefore(deadline));
        Path saved = Artifacts.image("canvas-timeout-" + button.name().toLowerCase(), shot);
        throw new IllegalStateException(String.format("%s not shown within %ds (best score %.2f, canvas saved to %s)",
                button, timeout.toSeconds(), best.score(), saved.toAbsolutePath()));
    }

    public void click(GameButton button) {
        BufferedImage shot = screenshot();
        Match m = ImageMatcher.find(shot, button.image(), MATCH_THRESHOLD)
                .orElseThrow(() -> new IllegalStateException(button + " not on screen"));
        clickAt(m.x() / shot.getWidth(), m.y() / shot.getHeight());
    }

    /** Sets the age slider and accepts. */
    public void acceptAgeGate(int age) {
        setAge(age);
        clickAgeAccept();
        waitUntilGone(GameButton.AGE_TITLE, Duration.ofSeconds(30));
    }

    /** Drags the age slider (0 to 55+) from 0. Positions measured on the 880px reference canvas. */
    public void setAge(int age) {
        waitFor(GameButton.AGE_TITLE, Duration.ofSeconds(30));
        double y = 211 / 495.0;
        double start = 332 / 880.0;
        double end = 548 / 880.0;
        double target = start + (end - start) * Math.min(age, 55) / 55.0;
        dragAt(start, y, target, y);
    }

    public void clickAgeAccept() {
        click(GameButton.AGE_ACCEPT);
    }

    /** Accept is grey-blue until an age is set, then green. */
    public boolean isAgeAcceptEnabled() {
        return isAgeAcceptEnabled(screenshot());
    }

    static boolean isAgeAcceptEnabled(BufferedImage canvas) {
        Match m = ImageMatcher.find(canvas, GameButton.AGE_ACCEPT.image(), MATCH_THRESHOLD)
                .orElseThrow(() -> new IllegalStateException("Accept button not on screen"));
        // sample the button background left of the label
        long g = 0, b = 0, n = 0;
        for (int y = (int) m.y() - 16; y < m.y() - 4; y++) {
            for (int x = (int) m.x() - 68; x < m.x() - 48; x++) {
                int rgb = canvas.getRGB(x, y);
                g += (rgb >> 8) & 0xff;
                b += rgb & 0xff;
                n++;
            }
        }
        return (g - b) / n > 60;
    }

    public void waitUntilGone(GameButton button, Duration timeout) {
        Instant deadline = Instant.now().plus(timeout);
        while (isShown(button)) {
            if (Instant.now().isAfter(deadline)) {
                throw new IllegalStateException(button + " still shown after " + timeout.toSeconds() + "s");
            }
            sleep(Duration.ofSeconds(1));
        }
    }

    /** Holds a key on the focused game (click the canvas first so Unity has focus). */
    public void holdKey(CharSequence key, Duration duration) {
        new Actions(driver).keyDown(key).pause(duration).keyUp(key).perform();
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
