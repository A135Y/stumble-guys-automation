package com.ayusuf.sg.email;

import com.ayusuf.sg.config.Config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;

/**
 * A mail.tm inbox whose Scopely account is linked to a game profile, saved by AUTH-04.
 * Location: LINKED_INBOX (default .local/linked-inbox.properties, git-ignored).
 */
public final class LinkedAccount {

    private LinkedAccount() {
    }

    public static Path file() {
        return Path.of(Config.find("linked.inbox").orElse(".local/linked-inbox.properties"));
    }

    public static void save(MailTmInbox inbox) {
        try {
            Files.createDirectories(file().toAbsolutePath().getParent());
            Files.writeString(file(), "address=" + inbox.address() + "\npassword=" + inbox.password() + "\n");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static Optional<MailTmInbox> load() {
        if (!Files.exists(file())) return Optional.empty();
        Properties p = new Properties();
        try (var in = Files.newInputStream(file())) {
            p.load(in);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return Optional.of(MailTmInbox.existing(p.getProperty("address"), p.getProperty("password")));
    }
}
