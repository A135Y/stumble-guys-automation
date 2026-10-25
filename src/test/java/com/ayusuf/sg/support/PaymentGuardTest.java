package com.ayusuf.sg.support;

import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Test(groups = "unit")
public class PaymentGuardTest {

    public void blocksPaymentConfirmationButtons() {
        assertThat(PaymentGuard.isConfirmText("Pay now")).isTrue();
        assertThat(PaymentGuard.isConfirmText("PAY £2.99")).isTrue();
        assertThat(PaymentGuard.isConfirmText("Buy now")).isTrue();
        assertThat(PaymentGuard.isConfirmText("Confirm purchase")).isTrue();
        assertThat(PaymentGuard.isConfirmText("Place order")).isTrue();
        assertThat(PaymentGuard.isConfirmText("Complete payment")).isTrue();
    }

    public void allowsShopNavigation() {
        assertThat(PaymentGuard.isConfirmText("£2.99")).isFalse();
        assertThat(PaymentGuard.isConfirmText("Gems")).isFalse();
        assertThat(PaymentGuard.isConfirmText("Payment methods")).isFalse();
        assertThat(PaymentGuard.isConfirmText("Close")).isFalse();
        assertThat(PaymentGuard.isConfirmText(null)).isFalse();
    }

    public void detectsCompletedOrderUrls() {
        assertThat(PaymentGuard.isCompletedUrl("https://pay.example.com/checkout/success")).isTrue();
        assertThat(PaymentGuard.isCompletedUrl("https://shop.example.com/thank-you")).isTrue();
        assertThat(PaymentGuard.isCompletedUrl("https://www.stumbleguys.com/shop")).isFalse();
        assertThat(PaymentGuard.isCompletedUrl("https://pay.example.com/checkout?next=success")).isFalse();
    }
}
