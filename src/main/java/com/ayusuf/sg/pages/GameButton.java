package com.ayusuf.sg.pages;

import com.ayusuf.sg.support.ImageMatcher;

import java.awt.image.BufferedImage;

/** Button images in src/main/resources/game, cut at {@link GameCanvas#REF_WIDTH}. */
public enum GameButton {
    WELCOME_TITLE("welcome-title"),
    WELCOME_START_PLAYING("welcome-start-playing"),
    WELCOME_CONTINUE_WITH_EMAIL("welcome-continue-with-email"),
    AGE_TITLE("age-title"),
    AGE_ACCEPT("age-accept"),
    DOWNLOAD_TITLE("download-title"),
    DOWNLOAD_OK("download-ok"),
    POPUP_CLOSE("popup-close"),
    POPUP_CLOSE_RED("popup-close-red"),
    LOBBY_PLAY("lobby-play"),
    LOBBY_CUSTOMIZE("lobby-customize"),
    LOBBY_EVENTS("lobby-events"),
    CUSTOMIZE_TITLE("customize-title"),
    EVENTS_TITLE("events-title"),
    SCREEN_BACK("screen-back");

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
