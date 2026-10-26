package com.ayusuf.sg.support;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.AndroidStartScreenRecordingOptions;
import org.openqa.selenium.WebDriver;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * One video per test (-Drecord=true), saved to the run's artifacts folder.
 * Desktop: ffmpeg screen capture (the terminal needs macOS Screen Recording permission).
 * Android: Appium's emulator screen recording.
 */
public interface ScreenRecorder {

    /** Stops and saves {@code <name>.mp4}; returns where, if a video was made. */
    Optional<Path> stop(String name);

    static ScreenRecorder start(WebDriver driver) {
        try {
            return driver instanceof AndroidDriver android ? new Android(android) : Desktop.start();
        } catch (RuntimeException e) {
            System.err.println("[record] not recording: " + e.getMessage());
            return name -> Optional.empty();
        }
    }

    final class Android implements ScreenRecorder {
        private final AndroidDriver driver;

        Android(AndroidDriver driver) {
            this.driver = driver;
            driver.startRecordingScreen(new AndroidStartScreenRecordingOptions().withTimeLimit(Duration.ofMinutes(10)));
        }

        @Override
        public Optional<Path> stop(String name) {
            try {
                byte[] mp4 = Base64.getMimeDecoder().decode(driver.stopRecordingScreen());
                return Optional.of(Artifacts.write(name + ".mp4", mp4));
            } catch (RuntimeException e) {
                System.err.println("[record] couldn't save Android recording: " + e.getMessage());
                return Optional.empty();
            }
        }
    }

    final class Desktop implements ScreenRecorder {
        private static final Pattern SCREEN = Pattern.compile("\\[(\\d+)] Capture screen 0");
        private final Process ffmpeg;
        private final Path temp;

        private Desktop(Process ffmpeg, Path temp) {
            this.ffmpeg = ffmpeg;
            this.temp = temp;
        }

        static Desktop start() {
            String device = screenDevice().orElseThrow(() -> new IllegalStateException(
                    "ffmpeg can't see the screen. Grant your terminal Screen Recording permission"
                            + " (System Settings > Privacy & Security > Screen Recording) and restart it."));
            try {
                Path temp = Files.createTempFile("sg-recording", ".mp4");
                Process p = new ProcessBuilder("ffmpeg", "-y", "-loglevel", "error",
                        "-f", "avfoundation", "-capture_cursor", "1", "-framerate", "15", "-i", device + ":none",
                        "-vf", "scale=1728:-2", "-c:v", "libx264", "-preset", "veryfast", "-crf", "28",
                        "-pix_fmt", "yuv420p", temp.toString())
                        .redirectErrorStream(true)
                        .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                        .start();
                return new Desktop(p, temp);
            } catch (IOException e) {
                throw new IllegalStateException("ffmpeg not found (brew install ffmpeg)", e);
            }
        }

        @Override
        public Optional<Path> stop(String name) {
            try (OutputStream in = ffmpeg.getOutputStream()) {
                in.write('q'); // ffmpeg finishes the file cleanly on 'q'
            } catch (IOException ignored) {
                // already exited
            }
            try {
                if (!ffmpeg.waitFor(15, TimeUnit.SECONDS)) ffmpeg.destroyForcibly();
                if (Files.size(temp) == 0) return Optional.empty();
                return Optional.of(Artifacts.write(name + ".mp4", Files.readAllBytes(temp)));
            } catch (IOException | InterruptedException e) {
                System.err.println("[record] couldn't save desktop recording: " + e.getMessage());
                return Optional.empty();
            } finally {
                try {
                    Files.deleteIfExists(temp);
                } catch (IOException ignored) {
                    // temp file
                }
            }
        }

        private static Optional<String> screenDevice() {
            try {
                Process p = new ProcessBuilder("ffmpeg", "-hide_banner", "-f", "avfoundation", "-list_devices", "true", "-i", "")
                        .redirectErrorStream(true).start();
                String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                p.waitFor(10, TimeUnit.SECONDS);
                Matcher m = SCREEN.matcher(out);
                return m.find() ? Optional.of(m.group(1)) : Optional.empty();
            } catch (IOException | InterruptedException e) {
                return Optional.empty();
            }
        }
    }
}
