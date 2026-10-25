package com.ayusuf.sg.tests;

import com.ayusuf.sg.pages.MobilePlayPage;
import com.ayusuf.sg.support.Artifacts;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** WGL-M1: mobile browsers get the app store links on /play, pointing at the real Stumble Guys apps. */
@MobileOnly("desktop gets the WebGL game on /play (WGL-01)")
@Test(groups = {"smoke", "core"})
public class MobilePlayPageTest extends BaseTest {

    public void playPageOffersTheStoreApps() {
        MobilePlayPage page = new MobilePlayPage(driver).open();

        assertThat(page.showsGooglePlayLink()).as("Google Play link to " + MobilePlayPage.ANDROID_APP_ID).isTrue();
        assertThat(page.showsAppStoreLink()).as("App Store link to " + MobilePlayPage.IOS_APP_ID).isTrue();
        assertThat(page.hasGameCanvas()).as("no WebGL game on mobile").isFalse();
        Artifacts.screenshot(driver, "wgl-m1-mobile-play");
    }
}
