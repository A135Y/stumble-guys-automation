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
| WGL-01 | Game loads to the welcome screen | Desktop | webgl |
| WGL-02 | Guest onboarding: Start Playing -> age gate -> past it to asset download | Desktop | webgl |
| WGL-04 | Age gate: Accept disabled and ignored at 0 years, enabled once an age is set | Desktop | webgl |
| WGL-05 | Gameplay: join a round, holding the up arrow moves the player | Desktop | webgl-slow (opt-in) |

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

### WGL-05 (implemented, `GameplayTest`, group `webgl-slow`)
Fresh browser every run, so it **downloads the 788 MB of game assets each time** (decided over caching a browser profile: keeps runs isolated, at the cost of time and bandwidth). Opt-in only: `./mvnw test -Dgroups=webgl-slow`. Joins a real match with a guest player.
1. Onboarding (as WGL-02) -> OK on Download Assets
2. Wait for the lobby PLAY button, closing promo popups (blue X) as they appear
3. PLAY -> wait for the round HUD ("Objective" / "Qualified", same on every map)
4. Measure screen change over 3s standing still, then over 3s holding the up arrow (the run key)
5. Expect: still in the round, and running changes the screen clearly more than standing (> 2x idle, min 8)

Found with the gameplay probe (2026-10-25): a "Dear Fred's Candy Countdown" promo covered the lobby after the download; round seen: Spin Go Round (race to finish, 12 qualify).

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
