package com.ayusuf.sg.tests;

import com.ayusuf.sg.pages.ShopPage;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Test(groups = "smoke")
public class ShopSmokeTest extends BaseTest {

    public void loggedOutPurchaseAsksToLogIn() {
        ShopPage shop = new ShopPage(driver).open();
        assertThat(shop.priceButtons()).as("price buttons").isNotEmpty();

        String price = shop.clickPrice(0);

        assertThat(price).matches("[£$€]\\d+[.,]\\d{2}");
        assertThat(shop.isLoginRequired()).as("login modal").isTrue();
    }
}
