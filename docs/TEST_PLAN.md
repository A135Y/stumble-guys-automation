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

## Suites

**Core** (`-Dgroups=core`) - run on desktop and Android. Covers the brief: login, one purchase flow stopping before payment, and in-game cases.

| ID | Test |
|---|---|
| AUTH-02 | Linked account logs in |
| AUTH-04 | Register -> link in the web game -> log in |
| PUR-01 | Shop -> Xsolla checkout, stops before payment |
| WGL-01 | Game loads to the welcome screen |
| WGL-04 | Age gate blocks Accept until an age is set |
| WGL-05 | Lobby -> Customize -> tab -> back |
| WGL-06 | Lobby -> Events -> Ranked tab -> back |

**Desktop extras** - kept as additional coverage, not ported to mobile: AUTH-01 (new email reaches "Almost there"), WGL-02 (onboarding reaches the asset download).

**Always run by default** (`./mvnw test`): unit tests (no browser) and smoke tests (no account).

The exploration probes used to capture each new screen before writing its test were removed once the tests existed; they're in the git history.

## Test cases

| ID | Title | Platforms | Group |
|---|---|---|---|
| AUTH-01 | New email reaches "Almost there" after sign-in | Desktop, Android | auth |
| AUTH-02 | Linked account logs in and header shows logged-in state | Desktop, Android | auth |
| AUTH-03 | Wrong code is rejected | Desktop | auth |
| AUTH-04 | Full flow: register -> link in game -> log in | Desktop | e2e-full (opt-in) |
| PUR-01 | Shop item checkout stops at payment step | Desktop, Android | purchase |
| WGL-01 | Game loads to the welcome screen | Desktop | webgl |
| WGL-02 | Guest onboarding: Start Playing -> age gate -> past it to asset download | Desktop | webgl |
| WGL-04 | Age gate: Accept disabled and ignored at 0 years, enabled once an age is set | Desktop | webgl |
| WGL-05 | Lobby: Customize opens, a category tab responds, Back returns to the lobby | Desktop | webgl-slow (opt-in) |
| WGL-06 | Lobby: Events opens, the Ranked tab responds, Back returns to the lobby | Desktop | webgl-slow (opt-in) |

Details are added as each case is implemented.

### AUTH-01 (implemented, `RegisterTest`)
1. Create a mail.tm inbox
2. Home -> menu -> Login -> Continue with email -> enter address -> Agree and get sign up link
3. Open the emailed link in a second tab, wait for "Success", switch back
4. Expect: back on stumbleguys.com, `idToken` in localStorage, "Almost there" popup, header still logged out

Verified with the probe on desktop Chrome (2026-10-24). No code step for new accounts. Test passed 2026-10-25 (15.8s).

### AUTH-04 (implemented, `FullAuthFlowTest`, group `e2e-full`)
1. New mail.tm inbox
2. `/play` -> welcome -> Continue with email -> Scopely popup (new account: Agree + link) -> popup closes
3. Age gate: slider to 25, Accept
4. Website login with the same email (returning: code)
5. Expect: `idToken` stored, no "Almost there", header logged in. Saves `auth04-logged-in.png`

Passed on desktop Chrome (2026-10-25) after fixing #6, #8, #9, #10; passed again after the #12 fix and speed-ups (63.7s, was 1m53s).

### PUR-01 (implemented, `PurchaseFlowTest`, group `purchase`)
Precondition (`LinkedAccountSetup`): a logged-in, game-linked account. Reuses `.local/linked-inbox.properties`; if missing or not logged in, links a new account in the web game and saves it. Doesn't depend on running AUTH-04 first.
1. `/shop` -> click the first price button (checked by `PaymentGuard`)
2. Xsolla checkout opens in `iframe.payment-container-iframe` (payment types, card fields, summary, **Pay**)
3. Expect: summary shows Total = clicked price, Card option, Pay button present. Nothing is filled in, Pay is never clicked.
4. Close the checkout; `PaymentGuard` confirms no window reached a completed-order page. Saves `pur01-checkout.png`

Passed on desktop Chrome (2026-10-25): checkout showed Total £2.99, Pay not clicked, header logged in.

### AUTH-02 (implemented, `LoginTest`)
Saved linked account -> email -> code -> logged in, no "Almost there". Passed 2026-10-25 (12.2s).

### WGL-02 / WGL-04 (implemented, `GameOnboardingTest`)
Guest player, no account. Found with the onboarding probe (2026-10-25):
- Start Playing on the welcome screen goes straight to the age gate
- Accept is grey-blue at 0 years and clicking it does nothing; it turns green once an age is set
- After Accept the game asks to **download 788 MB of assets** before the lobby

WGL-04: Accept disabled at 0 -> click -> still on age gate -> set 25 -> Accept enabled.
WGL-02: set 25 -> Accept -> "Download Assets" prompt with OK.

Both passed on desktop Chrome (2026-10-25): WGL-04 53.8s, WGL-02 58.0s (mostly the 30-45s game load).

### WGL-05 / WGL-06 (implemented, `GameLobbyTest`, group `webgl-slow`)
Lobby tests. The lobby sits behind a **788 MB asset download**, done in a fresh browser every run (decided over caching a browser profile: keeps runs isolated, at the cost of time and bandwidth). Opt-in only: `./mvnw test -Dgroups=webgl-slow`.

First tried a gameplay test (join a round, hold the up arrow). The gameplay probe got into a round once (Spin Go Round), but the WGL-05 run then sat on matchmaking waiting for players - depends on live servers and other people, so not a stable test. Switched to lobby screens, which open immediately and are deterministic. Promo popups over the lobby are closed as they appear (two styles seen: blue X "Dear Fred's Candy Countdown", red X "Chaotic Offers" - the latter has FREE claim buttons, never clicked).
1. Onboarding -> Download Assets OK -> wait until the lobby is clear (PLAY visible, no popup X; PLAY matches even dimmed behind a popup)
2. WGL-05: click CUSTOMIZE -> "CUSTOMIZE" screen -> click the second category tab -> screen changes -> back -> lobby clear
3. WGL-06: click EVENTS -> "LET'S PLAY" screen -> click the Ranked tab -> screen changes -> back -> lobby clear

"Screen changes" = change after the click is more than 3x the change over the same 2s without clicking (the screens animate by themselves). No equipping or buying. Back is pressed repeatedly (closing popups) until the lobby is clear, since sub-views can need more than one.

Seen: the Customize tab opens "Victory Animation", the Ranked tab opens "Ranked Season 27". The back arrow is drawn slightly differently there (match 0.85-0.95 vs 1.0 on the main screens), so it's matched at 0.8, with Escape as a fallback.

Lobby probe (2026-10-25): both screens opened; Escape also goes back, but the tests use the visible back arrow.

Both passed on desktop Chrome (2026-10-25) with the tab interactions: WGL-05 109s (Victory Animation, x51.8 vs idle), WGL-06 96s (Ranked, x10.9). Game load + 788 MB download + lobby is most of the time. Earlier runs failed on a Chaotic Offers popup sliding in after returning (lobby-ready now holds for two checks) and on the back arrow scoring under 0.9 on sub-views.

### Returning email (verified with the probe, used by AUTH-02)
1. Same entry as AUTH-01; no new-account step
2. Email "[NNNNNN] Your Scopely Account one-time code" -> type into the 6 boxes -> Sign in
3. Unlinked account: back on stumbleguys.com with "Almost there". Linked account: should be logged in (AUTH-02, needs a linked test account)

## Risks

| Risk | Mitigation |
|---|---|
| Scopely blocks disposable email domains | IMAP provider as a fallback (ADR 3) |
| Bot detection / captcha on sign-in | Run headed by default, document if it happens |
| Ads change the page layout | Wait on specific elements; canvas clicks relative to the canvas |
| Game UI changes (seasonal art) | Image matching on cropped buttons, not full screens |
| One session per account | Separate accounts per platform, no parallel runs on one account |
| Real accounts created on every full run | AUTH-04 is opt-in |
