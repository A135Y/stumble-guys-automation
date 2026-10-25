# Stumble Guys Automation

Automated tests for the Stumble Guys web portal (https://www.stumbleguys.com/), running on desktop Chrome and Chrome on Android.

> Work in progress.

## Running

Needs JDK 21 and Chrome.

```bash
./mvnw test                      # unit + smoke, desktop Chrome
./mvnw test -Dheadless=true
./mvnw test -Pandroid            # Chrome on Android (needs Appium + emulator, setup TBD)
```

Config: copy `.env.example` to `.env`, or pass `-Dkey=value`.

Desktop runs draw a red dot where the automation moves and clicks (Selenium doesn't move the real cursor). Turn off with `-Dshow.cursor=false`.

## Scope

- Register / login (Scopely ID, email + one-time code)
- Shop purchase flow, stopping before payment
- Bonus: two test cases against the WebGL game

## Docs

- `docs/investigation/` - notes from exploring the site before writing tests
- `docs/adr/` - decisions and why they were made
- `docs/TEST_PLAN.md` - test cases and scope
