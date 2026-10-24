package com.ayusuf.sg.email;

import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@Test(groups = "unit")
public class ScopelyEmailParserTest {

    public void realSignUpEmailHasVerifyLinkAndNoCode() {
        String html = load("scopely-signup.html");

        assertThat(ScopelyEmailParser.signInLink(html)).contains("https://id.scopely.com/email/verify?token=TESTTOKEN");
        assertThat(ScopelyEmailParser.signInLink(load("scopely-signup.txt")))
                .contains("https://id.scopely.com/email/verify?token=TESTTOKEN");
        assertThat(ScopelyEmailParser.oneTimeCode(html)).isEmpty();
    }

    public void findsSignInLinkInHtml() {
        assertThat(ScopelyEmailParser.signInLink(load("synthetic-signin.html")))
                .contains("https://id.scopely.com/email/verify/abc123?token=TESTTOKEN&state=xyz");
    }

    public void findsSignInLinkInPlainText() {
        assertThat(ScopelyEmailParser.signInLink(load("synthetic-signin.txt")))
                .contains("https://id.scopely.com/email/verify/abc123?token=TESTTOKEN");
    }

    public void findsCodeAndIgnoresCssColours() {
        assertThat(ScopelyEmailParser.oneTimeCode(load("synthetic-signin.html"))).contains("482913");
    }

    public void prefersNumberAfterTheWordCode() {
        assertThat(ScopelyEmailParser.oneTimeCode(load("synthetic-signin.txt"))).contains("104857");
    }

    public void ignoresNonScopelyLinks() {
        assertThat(ScopelyEmailParser.signInLink("<a href=\"https://evil.example/verify\">x</a>")).isEmpty();
    }

    public void emptyWhenNothingFound() {
        assertThat(ScopelyEmailParser.oneTimeCode("no digits here")).isEmpty();
        assertThat(ScopelyEmailParser.signInLink(null)).isEmpty();
    }

    private static String load(String name) {
        try (InputStream in = ScopelyEmailParserTest.class.getResourceAsStream("/emails/" + name)) {
            if (in == null) throw new IllegalArgumentException("Missing fixture " + name);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
