package com.ayusuf.sg.email;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Runs against a local fake of the mail.tm API. */
@Test(groups = "unit")
public class MailTmInboxTest {

    private HttpServer server;
    private String apiUrl;
    private final List<String> requests = new CopyOnWriteArrayList<>();
    private volatile String messagesJson = "[]";

    @BeforeMethod
    public void startFake() throws IOException {
        requests.clear();
        messagesJson = "[]";
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", this::handle);
        server.start();
        apiUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterMethod(alwaysRun = true)
    public void stopFake() {
        server.stop(0);
    }

    public void createsAccountOnFirstDomainAndLogsIn() {
        MailTmInbox inbox = MailTmInbox.create(apiUrl);

        assertThat(inbox.address()).startsWith("sg-").endsWith("@fake.test");
        assertThat(requests).containsExactly("GET /domains", "POST /accounts", "POST /token");
    }

    public void returnsFirstMatchingMessageAfterSince() {
        messagesJson = """
                [{"id":"old","createdAt":"2026-01-01T00:00:00+00:00"},
                 {"id":"m1","createdAt":"2099-01-01T00:00:00+00:00"}]""";
        MailTmInbox inbox = MailTmInbox.existing(apiUrl, "a@fake.test", "pw");

        EmailMessage msg = inbox.waitForMessage(Instant.parse("2026-06-01T00:00:00Z"),
                m -> m.subject().contains("Sign in"), Duration.ofSeconds(5));

        assertThat(msg.id()).isEqualTo("m1");
        assertThat(msg.html()).contains("482913");
        assertThat(ScopelyEmailParser.oneTimeCode(msg.body())).contains("482913");
        assertThat(requests).doesNotContain("GET /messages/old");
    }

    public void timesOutWhenNothingArrives() {
        MailTmInbox inbox = MailTmInbox.existing(apiUrl, "a@fake.test", "pw");

        assertThatThrownBy(() -> inbox.waitForMessage(Instant.now(), m -> true, Duration.ofSeconds(1)))
                .hasMessageContaining("No matching email for a@fake.test");
    }

    private void handle(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        requests.add(ex.getRequestMethod() + " " + path);

        String body = switch (path) {
            case "/domains" -> "[{\"domain\":\"fake.test\"}]";
            case "/accounts" -> "{\"id\":\"acc\"}";
            case "/token" -> "{\"token\":\"t\"}";
            case "/messages" -> messagesJson;
            case "/messages/m1" -> """
                    {"id":"m1","subject":"Sign in to Scopely","from":{"address":"no-reply@scopely.com"},
                     "text":"","html":["<p>Your code: <b>482913</b></p>"]}""";
            default -> null;
        };
        byte[] bytes = (body == null ? "{}" : body).getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json");
        ex.sendResponseHeaders(body == null ? 404 : 200, bytes.length);
        ex.getResponseBody().write(bytes);
        ex.close();
    }
}
