# 4. Hard stop before payment

Status: accepted

## Context

The purchase test must not complete a real purchase. "Just don't click the pay button" relies on everyone remembering that forever.

## Decision

The purchase test stops as soon as it reaches the payment step and asserts what's on that page (item, price, payment provider). On top of that, the page object for the payment step doesn't expose any method that submits payment, and a guard fails the test if the driver ever lands on a known payment-confirmation URL.

## Consequences

- No real charges even if someone later edits the test carelessly.
- The test proves the flow up to payment, not the payment itself. That's what the brief asks for.
