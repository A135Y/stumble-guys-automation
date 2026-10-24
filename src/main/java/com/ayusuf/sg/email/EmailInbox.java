package com.ayusuf.sg.email;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Predicate;

public interface EmailInbox {

    String address();

    /** Waits for the first message received after {@code since} that matches. */
    EmailMessage waitForMessage(Instant since, Predicate<EmailMessage> match, Duration timeout);
}
