package com.ayusuf.sg.tests;

import com.ayusuf.sg.flows.LinkedAccountSetup;
import com.ayusuf.sg.pages.CheckoutOverlay;
import com.ayusuf.sg.pages.ShopPage;
import com.ayusuf.sg.support.Artifacts;
import com.ayusuf.sg.support.PaymentGuard;
import com.ayusuf.sg.support.StepTimer;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PUR-01: shop item -> Xsolla checkout, stopping before payment.
 * Precondition: a logged-in, game-linked account (reused, or linked in the web game if missing).
 */
@Test(groups = {"purchase", "core"})
public class PurchaseFlowTest extends BaseTest {

    public void checkoutOpensAndStopsBeforePayment() {
        LinkedAccountSetup.signedIn(driver);
        StepTimer.step("signed in (precondition)");

        ShopPage shop = new ShopPage(driver).open();
        StepTimer.step("shop open");
        String price = shop.clickPrice(0);

        CheckoutOverlay checkout = new CheckoutOverlay(driver).waitUntilShown();
        StepTimer.step("checkout shown");
        Artifacts.screenshot(driver, "pur01-checkout");
        String form = checkout.text();

        assertThat(form).as("checkout summary").contains("Total").contains(price);
        assertThat(form).as("payment options").contains("Card");
        assertThat(checkout.hasPayButton()).as("stopped at Pay, not clicked").isTrue();
        PaymentGuard.assertNotPurchased(driver);
        StepTimer.step("checkout checked");

        checkout.close();
        StepTimer.step("checkout closed");
        assertThat(checkout.isShown()).as("checkout closed").isFalse();
        PaymentGuard.assertNotPurchased(driver);
    }
}
