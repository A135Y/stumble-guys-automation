package com.ayusuf.sg.tests;

import com.ayusuf.sg.email.MailTmInbox;
import com.ayusuf.sg.support.Artifacts;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** -Dprobe.inbox=path/to/inbox.properties reuses an inbox, otherwise creates one. Saved to the run folder either way. */
final class ProbeInbox {

    private ProbeInbox() {
    }

    static MailTmInbox get() {
        String saved = System.getProperty("probe.inbox");
        MailTmInbox inbox = saved == null ? MailTmInbox.create() : load(Path.of(saved));
        Artifacts.text("inbox.properties", "address=" + inbox.address() + "\npassword=" + inbox.password() + "\n");
        return inbox;
    }

    private static MailTmInbox load(Path file) {
        Properties p = new Properties();
        try (var in = Files.newInputStream(file)) {
            p.load(in);
        } catch (IOException e) {
            throw new UncheckedIOException("Can't read " + file.toAbsolutePath(), e);
        }
        return MailTmInbox.existing(p.getProperty("address"), p.getProperty("password"));
    }
}
