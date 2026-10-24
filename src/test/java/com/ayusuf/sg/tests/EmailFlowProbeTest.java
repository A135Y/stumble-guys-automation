package com.ayusuf.sg.tests;

import com.ayusuf.sg.email.EmailMessage;
import com.ayusuf.sg.email.MailTmInbox;
import com.ayusuf.sg.email.ScopelyEmailParser;
import com.ayusuf.sg.pages.AlmostTherePopup;
import com.ayusuf.sg.pages.HomePage;
import com.ayusuf.sg.pages.ScopelySignInPage;
import com.ayusuf.sg.support.Artifacts;
import org.openqa.selenium.JavascriptExecutor;
import org.testng.annotations.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * One-off exploration of the real email + code flow with a mail.tm inbox.
 * Creates real accounts, so it only runs with -Dgroups=probe. Output goes to target/artifacts/.
 */
@Test(groups = "probe")
public class EmailFlowProbeTest extends BaseTest {

    private static final Duration EMAIL_TIMEOUT = Duration.ofSeconds(120);

    private final List<String> log = new ArrayList<>();

    public void probeEmailSignIn() {
        MailTmInbox inbox = MailTmInbox.create();
        Artifacts.text("inbox.properties", "address=" + inbox.address() + "\npassword=" + inbox.password() + "\n");
        note("inbox: " + inbox.address());

        try {
            ScopelySignInPage signIn = new HomePage(driver).open().openMenu().clickLogin().continueWithEmail();
            capture("01-email-step");

            Instant sent = Instant.now().minusSeconds(5);
            signIn.submitEmail(inbox.address());
            sleep(3);
            capture("02-after-submit");

            EmailMessage first = saveEmail(inbox, sent, "email-1");
            Optional<String> link = ScopelyEmailParser.signInLink(first.body());
            Optional<String> code = ScopelyEmailParser.oneTimeCode(first.body());
            note("parsed link: " + link.orElse("none"));
            note("parsed code: " + code.orElse("none"));

            boolean codeStep = signIn.isCodeStepShown();
            note("code step shown after submit: " + codeStep);

            if (!codeStep && link.isPresent()) {
                Instant beforeLink = Instant.now().minusSeconds(2);
                driver.get(link.get());
                sleep(3);
                capture("03-after-link");
                codeStep = signIn.isCodeStepShown();
                note("code step shown after link: " + codeStep);
                if (codeStep) {
                    // the link may trigger a second email with the code
                    try {
                        EmailMessage second = saveEmail(inbox, beforeLink, "email-2");
                        Optional<String> secondCode = ScopelyEmailParser.oneTimeCode(second.body());
                        if (secondCode.isPresent()) code = secondCode;
                    } catch (IllegalStateException e) {
                        note("no second email: " + e.getMessage());
                    }
                }
            }

            if (codeStep) {
                capture("04-code-step");
                if (code.isPresent()) {
                    signIn.enterCode(code.get());
                    sleep(5);
                    capture("05-after-code");
                }
            }

            sleep(5);
            capture("06-final");
            note("final url host: " + driver.getCurrentUrl().replaceAll("[?#].*", ""));
            note("idToken stored: " + hasIdToken());
            note("almost there shown: " + new AlmostTherePopup(driver).isShown());
        } finally {
            Artifacts.text("summary.txt", String.join("\n", log) + "\n");
            System.out.println("Probe output: " + Artifacts.RUN_DIR.toAbsolutePath());
        }
    }

    private EmailMessage saveEmail(MailTmInbox inbox, Instant since, String name) {
        EmailMessage m = inbox.waitForMessage(since, x -> true, EMAIL_TIMEOUT);
        Artifacts.text(name + ".html", m.html());
        Artifacts.text(name + ".txt", m.text());
        note(name + ": from=" + m.from() + " subject=" + m.subject());
        return m;
    }

    private void capture(String step) {
        Artifacts.screenshot(driver, step);
        Artifacts.pageSource(driver, step);
        note(step + ": " + driver.getCurrentUrl().replaceAll("[?#].*", ""));
    }

    private boolean hasIdToken() {
        if (!driver.getCurrentUrl().contains("stumbleguys.com")) return false;
        return (Boolean) ((JavascriptExecutor) driver)
                .executeScript("return localStorage.getItem('com.scopely:scopelyid:idToken') !== null");
    }

    private void note(String line) {
        log.add(line);
        System.out.println("[probe] " + line);
    }

    private static void sleep(int seconds) {
        try {
            Thread.sleep(Duration.ofSeconds(seconds));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
