# Auth flow investigation

Date: 2026-10-24
Browser: Chrome (desktop)

## Problem

Signing up / logging in on stumbleguys.com with email never seemed to actually log me in. The email + code steps completed without errors, but the site kept showing the logged-out state. Before automating anything I wanted to understand why, otherwise the login tests would just be flaky from day one.

## How the login works

1. Home -> menu -> **Login** opens a modal with "Continue with Facebook" and "Continue with email".
2. "Continue with email" redirects to `id.scopely.com` (Scopely ID, which runs on Okta). It's a standard OAuth2 authorization code flow with PKCE:
   ```
   /oauth2/default/v1/authorize?response_type=code&scope=openid email profile ...
     &redirect_uri=https://www.stumbleguys.com&code_challenge_method=S256&prompt=login
   ```
3. There's no separate register page. The sign-in page says "Sign in or create a Scopely Account" and only asks for an email. No passwords.
4. If the email has no Scopely account yet, an extra step appears: "Let's create a Scopely Account!" with an optional marketing checkbox and an **"Agree and get sign up link"** button (accepts the ToS). No email is sent until that's clicked. Found when the first mail.tm probe run sat waiting for an email that was never sent.
5. You get an email: accept the link, then enter the one-time code.
6. Scopely redirects back to `https://www.stumbleguys.com/?code=...&state=...` and the site exchanges the code for tokens.

Things stored in `localStorage` on stumbleguys.com:

| Key | When |
|---|---|
| `com.scopely:scopelyid:pkce_code_verifier` | Written before redirecting to Scopely |
| `com.scopely:scopelyid:locationBeforeRedirect` | Written before redirecting to Scopely |
| `com.scopely:scopelyid:idToken` | Written after a successful sign-in |

`prompt=login` forces Scopely to re-authenticate every time, which is why every login asks for a new code. That's by design, not a bug.

## What was actually going wrong

After the first sign-in the `idToken` was there, so the Scopely part had worked. But the site showed an **"Almost there"** popup saying the Scopely account was almost connected and needed to be linked to a Stumble Guys account from the in-game settings first. The header still loaded `/images/menu/logged_out.png`.

So the website only logs you in if your Scopely account is linked to a game profile. A brand new account will always stop at "Almost there".

## Fix: link the account in the web game

The game runs in the browser at `/play` (Unity WebGL, canvas id `react-unity-webgl-canvas-1`), so no phone or PC client is needed.

1. Open `/play` and wait for it to load.
2. Age gate: set an age and accept the Terms of Service.
3. A guest player is created. Either use "Continue with email" on the welcome screen, or go to the lobby -> gear icon -> Settings -> "Save your progress with Scopely Account" -> **Connect**.
4. Same email + code flow as the website.
5. Go back to the website and log in again (email + code). This time it logs you in.

Linking on its own doesn't update an existing website session. You have to log in on the website again after linking.

## Other things found along the way

- **One session per account.** Logging in on the website in another browser kicked the game session in the first browser out with "MULTIPLE DEVICES CONNECTED - Please restart the game to reconnect". Desktop and Android runs can't share an account at the same time.
- **The login has to finish in the same browser that started it.** The PKCE verifier lives in that browser's localStorage. Opening the email link in a different browser (or the mail app's built-in one) would complete sign-in there instead. In tests the link has to be opened in the same WebDriver session, or the code typed into the original tab.
- **Starting a second login overwrites the verifier**, so the first attempt fails silently.
- Cookie banner (Usercentrics) shows on first visit and has to be dismissed.
- Pages load a lot of ad and tracking scripts, and the `/play` page has ads either side of the game canvas that change size. Waits need to target specific elements, and canvas clicks need to be relative to the canvas.
- The game's UI is drawn on the canvas, so Selenium can't find its buttons. Needs coordinate clicks or image matching.

## Impact on the tests

- Register test: new inbox -> email + code -> back on the site with an `idToken` and the "Almost there" prompt. That's the correct result for a new account.
- Full end-to-end: register -> link in the web game -> log in on the website -> logged in.
- Login test: uses an account that's already linked.
- Each platform (desktop / Android) needs its own linked account, or tests run one after another.
- Fresh browser profile per test so old verifiers/tokens don't leak between runs.

## Selectors (id.scopely.com)

The Scopely pages have `data-test-id` attributes, which are more stable than the generated class names:

| Element | Selector |
|---|---|
| Email field | `input[name='email']` |
| Continue / Agree and get sign up link | `button[data-test-id='site-email-input-submit-button']` (same button on both steps) |
| Step heading | `h1.title` ("Let's get you in!" / "Let's create a Scopely Account!") |
