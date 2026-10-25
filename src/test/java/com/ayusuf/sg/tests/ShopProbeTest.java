package com.ayusuf.sg.tests;

import com.ayusuf.sg.email.LinkedAccount;
import com.ayusuf.sg.email.MailTmInbox;
import com.ayusuf.sg.flows.EmailSignIn;
import com.ayusuf.sg.pages.ShopPage;
import com.ayusuf.sg.support.Artifacts;
import com.ayusuf.sg.support.PaymentGuard;
import org.openqa.selenium.JavascriptExecutor;
import org.testng.SkipException;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Exploration of what happens after clicking a price while logged in.
 * Clicks one price button and nothing else; only watches and captures. Runs with -Dgroups=probe.
 */
@Test(groups = "probe")
public class ShopProbeTest extends BaseTest {

    private final List<String> log = new ArrayList<>();

    public void probeAfterPriceClick() {
        MailTmInbox inbox = LinkedAccount.load()
                .orElseThrow(() -> new SkipException("No linked account at " + LinkedAccount.file()));
        note("account: " + inbox.address());

        try {
            new EmailSignIn(driver, inbox).signIn();
            ShopPage shop = new ShopPage(driver).open();
            page("s01-shop");

            String shopTab = driver.getWindowHandle();
            Set<String> before = driver.getWindowHandles();
            note("clicked price " + shop.clickPrice(0));

            for (int i = 1; i <= 10; i++) {
                sleep(3);
                PaymentGuard.assertNotPurchased(driver);
                note(String.format("t+%ds url=%s tabs=%d iframes=%s", i * 3, strip(driver.getCurrentUrl()),
                        driver.getWindowHandles().size(), iframeHosts()));
                page("s02-shop-" + i);
                for (String h : driver.getWindowHandles()) {
                    if (before.contains(h)) continue;
                    driver.switchTo().window(h);
                    page("s03-newtab-" + i);
                    driver.switchTo().window(shopTab);
                }
            }
        } finally {
            Artifacts.text("summary.txt", String.join("\n", log) + "\n");
            System.out.println("Probe output: " + Artifacts.RUN_DIR.toAbsolutePath());
        }
    }

    private Object iframeHosts() {
        return ((JavascriptExecutor) driver).executeScript(
                "return [...document.querySelectorAll('iframe')].filter(f => f.offsetParent && f.src)"
                        + ".map(f => { try { return new URL(f.src).host } catch (e) { return '?' } })"
                        + ".filter(h => !/doubleclick|google|ads|amazon|criteo|usercentrics/.test(h)).join(',')");
    }

    private void page(String name) {
        Artifacts.screenshot(driver, name);
        Artifacts.pageSource(driver, name);
        note(name + ": " + strip(driver.getCurrentUrl()));
    }

    private static String strip(String url) {
        return url.replaceAll("[?#].*", "");
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
