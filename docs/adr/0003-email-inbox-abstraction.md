# 3. Read login emails through an inbox interface

Status: accepted

## Context

Login is passwordless: email -> link -> one-time code, every time (see `docs/investigation/auth-flow.md`). Tests have to read real emails to get the link and code. Registration needs a new address each run; login needs one that already exists and is linked to a game profile.

## Decision

An `EmailInbox` interface with pluggable providers, picked by config (`EMAIL_PROVIDER`):

- **Mail.tm** (default) - free REST API, can create a new address on demand and log back into it later with its password. No setup needed for reviewers.
- **IMAP** - for a fixed mailbox (e.g. a Gmail with an app password) if Mail.tm domains get blocked.

The inbox waits for a message sent to its address after the test started, with a timeout, and extracts the link and code with regex.

## Consequences

- Default run needs no secrets.
- If Scopely starts rejecting Mail.tm domains, switching to IMAP is a config change, not a code change.
- Link/code extraction gets unit tests against saved sample emails, so a template change shows up as a clear unit test failure rather than a timeout in the UI test.
