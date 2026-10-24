# 5. Reviewers should be able to run everything

Status: accepted

## Context

Whoever reviews this needs to run the tests on their own machine without help from me.

## Decision

- Maven wrapper (`./mvnw`) so only a JDK is needed for desktop. Selenium Manager fetches chromedriver.
- Appium server and the UiAutomator2 driver pinned in `package.json`; the framework starts the Appium server itself.
- A `Makefile` with `doctor`, `setup-android`, `test-desktop`, `test-android`, `test-all`.
- Config precedence: `config/default.properties` < `.env` < environment variables < `-D` flags. Every key documented in `.env.example`.
- Preflight checks. Anything missing (no emulator, no credentials) skips the affected tests with a reason that says how to fix it, instead of failing with a stack trace.
- Each run writes screenshots, videos and logs to `target/artifacts/<timestamp>/`.
- GitHub Actions runs the desktop suite headless to prove it works on a clean machine.

## Consequences

- More setup work up front, but every feature after this runs through the same harness.
- Tests that create real accounts (full register -> link -> login) go in an opt-in TestNG group so a normal run doesn't create accounts every time.
