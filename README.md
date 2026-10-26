# Stumble Guys Automation

[![Unit tests](https://github.com/A135Y/stumble-guys-automation/actions/workflows/unit-tests.yml/badge.svg)](https://github.com/A135Y/stumble-guys-automation/actions/workflows/unit-tests.yml)

UI tests for https://www.stumbleguys.com on desktop Chrome and Chrome on Android.
Java 21, Selenium 4, Appium 3, TestNG, Maven.

## What's tested

| ID | Test | Desktop | Android |
|---|---|---|---|
| AUTH-02 | Linked account logs in (email + one-time code) | ✓ | ✓ |
| AUTH-04 | New email → link account in the web game → log in | ✓ | skipped¹ |
| PUR-01 | Shop item → Xsolla checkout, **stops before payment** | ✓ | ✓ |
| WGL-01 | Game loads to the welcome screen | ✓ | skipped¹ |
| WGL-04 | Age gate blocks Accept until an age is set | ✓ | skipped¹ |
| WGL-05 | Lobby → Customize → tab → back | ✓ | skipped¹ |
| WGL-06 | Lobby → Events → Ranked tab → back | ✓ | skipped¹ |
| WGL-M1 | `/play` offers the Google Play / App Store apps | – | ✓ |

¹ Mobile browsers don't get the web game; `/play` shows app store links instead.

**Videos of each test running** (desktop and Android): [Test recordings release](https://github.com/A135Y/stumble-guys-automation/releases/tag/recordings).

Also: AUTH-01 (new email reaches "Almost there") and WGL-02 (onboarding → asset download) on desktop, plus unit and smoke tests. Steps for each test: [docs/TEST_PLAN.md](docs/TEST_PLAN.md).

## Setup

Desktop: **JDK 21** and **Chrome**. The Maven wrapper fetches dependencies and Selenium fetches chromedriver.

Android (macOS, needs Homebrew and Node 20+):

```bash
brew install --cask android-commandlinetools
./scripts/setup-android.sh
```

The script asks you to accept the Android SDK licences, installs the emulator and Android 15 with Chrome, creates an emulator called `sg-pixel`, and installs the Appium version pinned in `package.json`. Tests start Appium and the emulator themselves.

Optional config: copy `.env.example` to `.env`, or pass `-Dkey=value`.

## Running

```bash
./mvnw test                                  # unit + smoke tests, no accounts, ~30s
./mvnw test -Dgroups=e2e-full                # AUTH-04 first: creates the linked test account the others use
./mvnw test -Dgroups=core                    # core tests, desktop
./mvnw test -Dgroups=core -Pandroid          # core tests, Android
./mvnw test -Dgroups=core -Dtest=PurchaseFlowTest   # one class
./mvnw test -Dgroups=core -DexcludedGroups=webgl-slow,e2e-full   # core without the 788 MB lobby tests and new accounts
```

`core` includes AUTH-04 (creates a real account) and WGL-05/06 (788 MB download each). AUTH-02 and PUR-01 create the linked account themselves on desktop if none is saved, so the order doesn't matter.

Flags: `-Dheadless=true`, `-Dwindow.maximized=true` (bigger window), `-Dshow.cursor=false` (hides the red dot that shows where the test clicks), `-Drecord=true` (one video per test; on desktop the terminal needs macOS Screen Recording permission and `ffmpeg`).

Screenshots, step timings and (Android) `appium.log` go to `target/artifacts/<timestamp>/`. Test reports: `target/surefire-reports/index.html` (TestNG) and `emailable-report.html`.

CI runs the unit tests on every push and PR (`.github/workflows/unit-tests.yml`). The browser suites run against the live site with real accounts, so they run locally.

## Browsers and devices

Tested on macOS 26 (Apple Silicon):

| Platform | Browser | Driver |
|---|---|---|
| Desktop | Chrome 155 | Selenium 4.33 |
| Android 15 emulator (Pixel 7) | Chrome 124 | Appium 3.8.0 + UiAutomator2 8.7.0 |

## Assumptions and limitations

- **Live production site.** Tests create real Scopely accounts with disposable mail.tm inboxes; if mail.tm stops working or gets blocked, sign-up/login tests can't run. The linked account is saved in `.local/` (git-ignored).
- **No purchases.** PUR-01 stops at the checkout; a guard refuses to click Pay-type buttons and fails if a page looks like a completed order.
- **Login needs a game-linked account.** A new account stops at "Almost there" until it's linked in the game ([details](docs/investigation/auth-flow.md)). Run AUTH-04 on desktop before AUTH-02 / PUR-01 on Android.
- **One session per account.** Don't run desktop and Android with the same account at the same time. To run both at once, give Android its own account: add `-Dlinked.inbox=.local/android-inbox.properties` to a desktop AUTH-04 run and to the Android runs.
- **Web game is desktop only**, and drawn on a canvas, so it's tested by matching button images. New seasonal art or promo popups can need new images (`src/main/resources/game`).
- **Slow in-game tests.** A fresh browser every run means WGL-05/06 download ~788 MB each time; game load depends on the network (20–140s seen).
- **Not covered:** matchmaking and real rounds (live servers, other players), Facebook login, native apps. Ads aren't blocked ([#7](https://github.com/A135Y/stumble-guys-automation/issues/7)).
- Built and tested on macOS only; the Android setup script needs Homebrew.

## Project layout

```
src/main/java/.../config   settings (.env, -D overrides)
             .../driver    desktop Chrome and Android (Appium) drivers
             .../pages     page objects, incl. GameCanvas for the WebGL game
             .../flows     sign-in, game linking, linked-account setup
             .../email     mail.tm inbox, Scopely email parsing
             .../support   image matching, payment guard, screenshots
src/test/java/.../tests    test cases
docs/                      test plan, decisions (adr/), investigation notes
```
