package com.ayusuf.sg.support;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Run output under {@code target/artifacts/<timestamp>/}. */
public final class Artifacts {

    public static final Path RUN_DIR = Path.of("target", "artifacts",
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")));

    private Artifacts() {
    }

    public static Path screenshot(WebDriver driver, String name) {
        return write(name + ".png", ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES));
    }

    public static Path pageSource(WebDriver driver, String name) {
        return text(name + ".html", driver.getPageSource());
    }

    public static Path text(String fileName, String content) {
        return write(fileName, content == null ? new byte[0] : content.getBytes(StandardCharsets.UTF_8));
    }

    private static Path write(String fileName, byte[] bytes) {
        try {
            Files.createDirectories(RUN_DIR);
            Path file = RUN_DIR.resolve(fileName);
            Files.write(file, bytes);
            return file;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
