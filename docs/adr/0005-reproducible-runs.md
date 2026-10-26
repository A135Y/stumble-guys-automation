# 5. Reviewers should be able to run everything

Status: accepted

## Context

Whoever reviews this needs to run the tests on their own machine without help from me.

## Decision

- Maven wrapper (`./mvnw`) so only a JDK is needed for desktop. Selenium Manager fetches chromedriver.
- Appium server and the UiAutomator2 driver pinned in `package.json`; the framework starts the Appium server and the emulator itself. `scripts/setup-android.sh` installs everything Android needs.
- Config precedence: `config/default.properties` < `.env` < environment variables < `-D` flags. Every key documented in `.env.example`.
- Missing preconditions skip the affected tests with a reason that says how to fix it (e.g. no linked account, desktop-only tests on Android), instead of failing with a stack trace.
- Each run writes screenshots, step timings and logs to `target/artifacts/<timestamp>/`.

## Consequences

- More setup work up front, but every test runs through the same harness.
- Tests that create real accounts (full register -> link -> login) go in their own TestNG group (`e2e-full`), so the default `./mvnw test` creates none. AUTH-04 is also in `core` (the suite that covers the brief) and can be excluded with `-DexcludedGroups=e2e-full`.
