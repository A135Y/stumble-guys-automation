package com.ayusuf.sg.support;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chromium.ChromiumDriver;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/** Visible dot for the synthetic mouse (Selenium doesn't move the real cursor). Desktop Chrome only. */
public final class CursorOverlay {

    private static final String ID = "__sg_cursor";

    private CursorOverlay() {
    }

    public static void install(WebDriver driver) {
        if (driver instanceof ChromiumDriver chrome) {
            chrome.executeCdpCommand("Page.addScriptToEvaluateOnNewDocument", Map.of("source", script()));
        }
    }

    /** Hides the dot so it can't end up in a screenshot used for image matching. */
    public static void setVisible(WebDriver driver, boolean visible) {
        ((JavascriptExecutor) driver).executeScript(
                "const d = document.getElementById(arguments[0]); if (d) d.style.visibility = arguments[1];",
                ID, visible ? "visible" : "hidden");
    }

    private static String script() {
        try (InputStream in = CursorOverlay.class.getResourceAsStream("/cursor-overlay.js")) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
