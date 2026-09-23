## Context

See `proposal.md` for motivation and
`specs/disabled-account-login/spec.md` for the behavioral contract.

The EyeSeeTea fork adds a two-step username/password flow that is absent from
upstream: `LogInCall.loginInDhis2AndGetUser()` first calls
`UserNetworkHandler.login()` (`/api/auth/login`), examines the resulting
`LoginResponse`, stores credentials, and then calls `getUser(true)` (`/api/me`).
The response-status check is currently named `generate2FAErrorIfRequired()` and
only recognizes supported 2FA statuses. `ACCOUNT_DISABLED` therefore falls
through as if it were `SUCCESS`.

The later `/api/me` 401 is caught by `UserAccountDisabledErrorCatcher`, which
removes the current account and emits `AccountDeletionReason.ACCOUNT_DISABLED`.
That behavior is intended for an account disabled during an established session,
not for credentials rejected before a new session has been established. Applying
it to the login response can discard locally stored, unsynchronized data and
makes a global deletion event observable before the login call returns its error.

This change affects only the `user` module. It does not use or modify
`aggregatedSyncState`, trimming settings, any domain wiper, or the shared
`ModuleWiper` interface.

## Goals / Non-Goals

**Goals:**

- Classify `ACCOUNT_DISABLED` at the first response boundary.
- Reuse `D2ErrorCode.USER_ACCOUNT_DISABLED` so the public SDK contract remains
  compatible.
- Prevent credential persistence and `/api/me` retrieval for this terminal
  result.
- Preserve local account data and avoid account-deletion events for this failed
  login attempt.
- Cover the behavior at unit level, including negative verification that
  `getUser(true)` is not invoked.

**Non-Goals:**

- Changing upstream cleanup when a disabled account is detected during an
  established session.
- Changing downstream app dialogs, navigation, or session-event subscriptions.
- Changing the wire value returned by DHIS2 or the existing
  `USER_ACCOUNT_DISABLED` failure exposed to login callers.
- Refactoring all login statuses into a new public model.
- Changing upstream's direct Basic Auth login flow.

## Decisions

### Map the wire status to the existing SDK error before storing credentials

Represent `ACCOUNT_DISABLED` as a private protocol constant and extend the
response-status validation immediately after `/api/auth/login` so that value
creates a `D2Error` with `D2ErrorCode.USER_ACCOUNT_DISABLED`. Rename the helper
from its 2FA-specific name to reflect that it validates all terminal login
statuses.

This lets the existing `LogInCall` catch path return the same public error
consumers already understand. Because the exception is raised before
`credentialsSecureStore.set(credentials)`, the attempt never becomes a temporary
authenticated session and `/api/me` is never called.

Alternative considered: wait for `/api/me` to return 401 and improve only the
global catcher. Rejected because it retains an unnecessary request, delays the
correct result, and ignores the authoritative status already returned by the
login endpoint.

`ACCOUNT_DISABLED` represents the protocol status only and therefore does not
belong in the public `D2ErrorCode` API. The thrown error remains
`USER_ACCOUNT_DISABLED`, which already expresses the SDK-domain condition and is
mapped by existing consumers.

### Do not apply established-session cleanup to a login rejection

The response-status validator only classifies and throws; it does not delete,
log out, or emit. `handleOnlineException()` returns the disabled-account error
without invoking `AccountManagerImpl`, regardless of whether the same user has
local account data.

Credentials submitted to `logIn()` describe an attempt, not proof that the local
account should be removed. Keeping that data allows a temporarily disabled user
to sign in and synchronize pending changes after the account is re-enabled. The
existing `UserAccountDisabledErrorCatcher` behavior for an established session is
outside this change and remains untouched.

Alternative considered: invoke `UserAccountDisabledErrorCatcher` directly for
the HTTP 200 response. Rejected because that catcher is designed for failed HTTP
responses and would couple a successful transport response to HTTP-error
handling solely to reuse a side effect.

### Use focused unit tests rather than an instrumented database test

`LogInCallUnitShould` observes the orchestration contract: returned error code,
absence of `getUser(true)`, absence of credential and authenticated-user writes,
and absence of interaction with `AccountManagerImpl`. No Room integration test
is required because local account storage is deliberately not touched.

## Risks / Trade-offs

- **[Risk]** A future DHIS2 version changes the disabled wire status. → Keep the
  mapping explicit and backed by the captured `ACCOUNT_DISABLED` regression
  fixture; add new aliases only when supported by an observed server contract.
- **[Risk]** A consumer expects a deletion event for every disabled error. → Keep
  that event limited to the existing established-session catcher; document that
  a rejected login returns only `USER_ACCOUNT_DISABLED`.
- **[Risk]** Renaming the validation helper creates merge conflicts with future
  EyeSeeTea 2FA updates. → Keep the refactor local to `LogInCall` and its unit
  tests; no public API changes.
- **[Trade-off]** Local data remains present while the account is disabled. This
  avoids losing unsynchronized work and permits recovery after re-enablement;
  access control while logged out remains the downstream app's responsibility.

## Migration Plan

1. Implement and test the change on the branch based on
   `1.14.2-eyeseetea-fork-2`.
2. Publish the next EyeSeeTea patch artifact from that line.
3. Upgrade downstream apps to the new artifact and verify the disabled-account
   flow against PROD-INDIV/UAT: one `/api/auth/login` request, no `/api/me`
   request, and a visible disabled-account result.
4. Roll back by restoring the previous SDK artifact version; there are no schema
   or stored-data migrations.
