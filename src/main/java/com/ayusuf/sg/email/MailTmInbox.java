package com.ayusuf.sg.email;

import com.ayusuf.sg.config.Config;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Map;
import java.util.function.Predicate;

/** Disposable inbox on mail.tm (https://docs.mail.tm). */
public class MailTmInbox implements EmailInbox {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Duration POLL_INTERVAL = Duration.ofSeconds(1);

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final String apiUrl;
    private final String address;
    private final String password;
    private String token;

    private MailTmInbox(String apiUrl, String address, String password) {
        this.apiUrl = apiUrl.replaceAll("/+$", "");
        this.address = address;
        this.password = password;
    }

    /** Creates a new random address. */
    public static MailTmInbox create() {
        return create(defaultApiUrl());
    }

    static MailTmInbox create(String apiUrl) {
        MailTmInbox inbox = new MailTmInbox(apiUrl, null, randomHex(12));
        String domain = inbox.get("/domains").path(0).path("domain").asText();
        if (domain.isEmpty()) throw new IllegalStateException("mail.tm returned no domains");

        MailTmInbox created = new MailTmInbox(apiUrl, "sg-" + randomHex(5) + "@" + domain, inbox.password);
        created.post("/accounts", Map.of("address", created.address, "password", created.password));
        created.login();
        return created;
    }

    /** Logs back into an address created earlier. */
    public static MailTmInbox existing(String address, String password) {
        return existing(defaultApiUrl(), address, password);
    }

    static MailTmInbox existing(String apiUrl, String address, String password) {
        MailTmInbox inbox = new MailTmInbox(apiUrl, address, password);
        inbox.login();
        return inbox;
    }

    @Override
    public String address() {
        return address;
    }

    public String password() {
        return password;
    }

    @Override
    public EmailMessage waitForMessage(Instant since, Predicate<EmailMessage> match, Duration timeout) {
        Instant deadline = Instant.now().plus(timeout);
        while (Instant.now().isBefore(deadline)) {
            for (JsonNode summary : get("/messages")) {
                Instant received = OffsetDateTime.parse(summary.path("createdAt").asText()).toInstant();
                if (received.isBefore(since)) continue;

                EmailMessage message = fetch(summary.path("id").asText(), received);
                if (match.test(message)) return message;
            }
            sleep(POLL_INTERVAL);
        }
        throw new IllegalStateException("No matching email for " + address + " within " + timeout.toSeconds() + "s");
    }

    private EmailMessage fetch(String id, Instant received) {
        JsonNode m = get("/messages/" + id);
        StringBuilder html = new StringBuilder();
        m.path("html").forEach(part -> html.append(part.asText()));
        return new EmailMessage(id, m.path("from").path("address").asText(), m.path("subject").asText(),
                m.path("text").asText(), html.toString(), received);
    }

    private void login() {
        token = post("/token", Map.of("address", address, "password", password)).path("token").asText();
        if (token.isEmpty()) throw new IllegalStateException("mail.tm login failed for " + address);
    }

    private JsonNode get(String path) {
        return send(request(path).GET().build());
    }

    private JsonNode post(String path, Map<String, String> body) {
        try {
            HttpRequest.Builder req = request(path).header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body)));
            return send(req.build());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private HttpRequest.Builder request(String path) {
        HttpRequest.Builder req = HttpRequest.newBuilder(URI.create(apiUrl + path))
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "application/json");
        if (token != null) req.header("Authorization", "Bearer " + token);
        return req;
    }

    private JsonNode send(HttpRequest request) {
        try {
            HttpResponse<String> res = http.send(request, HttpResponse.BodyHandlers.ofString());
            for (int retry = 0; res.statusCode() == 429 && retry < 3; retry++) {
                sleep(Duration.ofSeconds(2));
                res = http.send(request, HttpResponse.BodyHandlers.ofString());
            }
            if (res.statusCode() >= 400) {
                throw new IllegalStateException("mail.tm " + request.method() + " " + request.uri().getPath()
                        + " -> " + res.statusCode() + ": " + res.body());
            }
            return res.body().isBlank() ? JSON.nullNode() : JSON.readTree(res.body());
        } catch (IOException e) {
            throw new IllegalStateException("mail.tm request failed: " + request.uri(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private static String defaultApiUrl() {
        return Config.find("mailtm.api.url").orElse("https://api.mail.tm");
    }

    private static String randomHex(int bytes) {
        byte[] b = new byte[bytes];
        new SecureRandom().nextBytes(b);
        return HexFormat.of().formatHex(b);
    }

    private static void sleep(Duration d) {
        try {
            Thread.sleep(d);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
