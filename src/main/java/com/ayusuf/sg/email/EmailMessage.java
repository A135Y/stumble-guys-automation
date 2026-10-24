package com.ayusuf.sg.email;

import java.time.Instant;

public record EmailMessage(String id, String from, String subject, String text, String html, Instant receivedAt) {

    /** HTML if there is any, otherwise plain text. */
    public String body() {
        return html != null && !html.isBlank() ? html : text;
    }
}
