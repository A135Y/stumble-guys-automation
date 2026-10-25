# 3. Read login emails through an inbox interface

Status: accepted

## Context

Login is passwordless: email -> link -> one-time code (see `docs/investigation/auth-flow.md`). Tests have to read real emails to get the link and code. Registration needs a new address each run; login needs one that already exists and is linked to a game profile.

## Decision

An `EmailInbox` interface, implemented with **Mail.tm**: a free REST API that can create a new address on demand and log back into it later with its password. No setup needed for reviewers.

The inbox waits for a message sent to its address after the test started, with a timeout, and extracts the link and code with regex.

## Consequences

- Runs need no secrets.
- Tests depend on Mail.tm being available and accepted by Scopely.
- Link/code extraction has unit tests against saved real emails, so a template change shows up as a clear unit test failure rather than a timeout in the UI test.
