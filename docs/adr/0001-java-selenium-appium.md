# 1. Java + Selenium + Appium

Status: accepted

## Context

The brief allows Python or Java, with Selenium or Playwright, plus Appium for mobile. Tests need to run on desktop Chrome and on a mobile browser.

## Decision

Java 21, Selenium 4, Appium java-client, TestNG, Maven.

Selenium over Playwright because Appium speaks the same WebDriver protocol. Page objects take a `WebDriver` and don't care whether it's ChromeDriver on desktop or an Appium session driving Chrome on Android. With Playwright I'd have two separate driver stacks and two sets of waits.

Java because it's what I'm most comfortable with for test frameworks, and TestNG gives groups, parallel config and listeners out of the box.

## Consequences

- One page object layer for both platforms; only the driver factory differs.
- Selenium Manager handles chromedriver for desktop, so there's no manual driver download.
- More boilerplate than Python, but that's acceptable.
