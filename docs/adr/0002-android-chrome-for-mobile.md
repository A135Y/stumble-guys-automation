# 2. Android + Chrome for mobile

Status: accepted

## Context

Mobile tests need one platform, iOS or Android.

## Decision

Android emulator with Chrome, driven through Appium's UiAutomator2 driver.

- iOS Safari needs Xcode, a simulator and Safari WebDriver setup, which a reviewer may not have and which only works on a Mac.
- Android tooling runs on macOS, Windows and Linux.
- WebGL support in Chrome on Android is more predictable than mobile Safari.

## Consequences

- Reviewers need the Android SDK and an emulator image with Chrome (Google APIs / Play Store image). A setup script will handle as much of this as possible.
- Appium can download the matching chromedriver automatically (`chromedriverAutodownload`).
