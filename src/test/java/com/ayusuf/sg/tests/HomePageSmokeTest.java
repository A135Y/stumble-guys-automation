package com.ayusuf.sg.tests;

import com.ayusuf.sg.pages.HomePage;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Test(groups = "smoke")
public class HomePageSmokeTest extends BaseTest {

    public void homePageLoads() {
        HomePage home = new HomePage(driver).open();

        assertThat(home.title()).contains("Stumble Guys");
        assertThat(home.isPlayLinkVisible()).as("Play link visible").isTrue();
        assertThat(home.isLoggedOut()).as("starts logged out").isTrue();
    }

    public void menuShowsLoginButton() {
        HomePage home = new HomePage(driver).open().openMenu();

        assertThat(home.isLoginButtonVisible()).as("Login button visible").isTrue();
    }
}
