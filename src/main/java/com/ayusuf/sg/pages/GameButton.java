package com.ayusuf.sg.pages;

import com.ayusuf.sg.support.ImageMatcher;

import java.awt.image.BufferedImage;

/** Button images in src/main/resources/game, cut at {@link GameCanvas#REF_WIDTH}. */
public enum GameButton {
    WELCOME_TITLE("welcome-title"),
    WELCOME_START_PLAYING("welcome-start-playing"),
    WELCOME_CONTINUE_WITH_EMAIL("welcome-continue-with-email");

    private final String file;
    private BufferedImage image;

    GameButton(String file) {
        this.file = file;
    }

    public synchronized BufferedImage image() {
        if (image == null) image = ImageMatcher.load("/game/" + file + ".png");
        return image;
    }
}
