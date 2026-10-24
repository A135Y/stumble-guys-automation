# Test plan

## Scope

In scope:
- Register and login on stumbleguys.com via Scopely ID (email + link + code)
- Linking a Scopely account to a game profile in the web game (needed for login to work, see `docs/investigation/auth-flow.md`)
- One shop purchase flow, stopping before payment
- Two test cases against the WebGL game at `/play`

Out of scope:
- Facebook login
- Completing any payment
- Gameplay beyond the menus (matches, controls)
- Native mobile apps (only the mobile web browser)

## Environments

| Platform | Browser | Driver |
|---|---|---|
| Desktop (macOS / Windows / Linux) | Chrome | Selenium (ChromeDriver via Selenium Manager) |
| Android emulator | Chrome | Appium UiAutomator2 |

## Test data

- Registration: a new Mail.tm inbox per run.
- Login / purchase: an account that is already linked to a game profile. One per platform, because an account can only have one active session.

## Test cases

| ID | Title | Platforms | Group |
|---|---|---|---|
| AUTH-01 | New email reaches "Almost there" after sign-in | Desktop, Android | auth |
| AUTH-02 | Linked account logs in and header shows logged-in state | Desktop, Android | auth |
| AUTH-03 | Wrong code is rejected | Desktop | auth |
| AUTH-04 | Full flow: register -> link in game -> log in | Desktop | e2e-full (opt-in) |
| PUR-01 | Shop item checkout stops at payment step | Desktop, Android | purchase |
| WGL-01 | Game loads and shows the age gate | Desktop | webgl |
| WGL-02 | Accepting the age gate reaches the welcome / lobby screen | Desktop | webgl |

Details (steps, expected results) will be added to each case as it's implemented.

## Risks

| Risk | Mitigation |
|---|---|
| Scopely blocks disposable email domains | IMAP provider as a fallback (ADR 3) |
| Bot detection / captcha on sign-in | Run headed by default, document if it happens |
| Ads change the page layout | Wait on specific elements; canvas clicks relative to the canvas |
| Game UI changes (seasonal art) | Image matching on cropped buttons, not full screens |
| One session per account | Separate accounts per platform, no parallel runs on one account |
| Real accounts created on every full run | AUTH-04 is opt-in |
