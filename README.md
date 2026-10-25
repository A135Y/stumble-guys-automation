# Stumble Guys Automation

Automated tests for the Stumble Guys web portal (https://www.stumbleguys.com/), running on desktop Chrome and Chrome on Android.

> Work in progress.

## Running

Needs JDK 21 and Chrome.

```bash
./mvnw test                      # unit + smoke, desktop Chrome, no accounts
./mvnw test -Dgroups=core        # the brief's test cases (creates/uses test accounts, downloads game assets)
./mvnw test -Dheadless=true
./mvnw test -Pandroid            # Chrome on Android (needs Appium + emulator, setup TBD)
```

Test cases and which run where: `docs/TEST_PLAN.md`.

### Android

One-time setup (macOS; needs Homebrew and Node 20+):

```bash
brew install --cask android-commandlinetools
./scripts/setup-android.sh       # accept the SDK licences when asked
```

This installs the emulator, Android 15 with Chrome, creates an emulator called `sg-pixel`, and installs the Appium server and Android driver pinned in `package.json` (Appium 3.8.0, UiAutomator2 8.7.0). The tests start Appium themselves and boot `sg-pixel` if no emulator is running (first boot takes ~1 min). Appium's log is saved as `appium.log` in the run's artifacts folder.

```bash
./mvnw test -Pandroid -Dgroups=smoke
```

The web game isn't offered on mobile browsers (`/play` shows app store links), so the in-game tests and AUTH-04 skip on Android. Run AUTH-04 on desktop first so mobile has a linked account for login and purchase.

Config: copy `.env.example` to `.env`, or pass `-Dkey=value`.

Desktop runs draw a red dot where the automation moves and clicks (Selenium doesn't move the real cursor). Turn off with `-Dshow.cursor=false`.

The browser opens at a fixed 1440x900 so the layout is the same on every machine. For a bigger window (e.g. recording), use `-Dwindow.maximized=true`.

## Scope

- Register / login (Scopely ID, email + one-time code)
- Shop purchase flow, stopping before payment
- Bonus: two test cases against the WebGL game

## Docs

- `docs/investigation/` - notes from exploring the site before writing tests
- `docs/adr/` - decisions and why they were made
- `docs/TEST_PLAN.md` - test cases and scope
