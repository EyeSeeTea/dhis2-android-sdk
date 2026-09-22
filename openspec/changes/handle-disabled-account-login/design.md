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
`LogInCall.handleOnlineException()` can then request disabled-account deletion
again using the attempted credentials. Besides the unnecessary request, this
late path makes a global deletion event observable before the original login
call has returned its error.

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
- Keep disabled-account cleanup centralized in the existing `LogInCall` online
  error path, so one failed attempt initiates cleanup at most once.
- Cover the behavior at unit level, including negative verification that
  `getUser(true)` is not invoked.

**Non-Goals:**

- Changing whether disabled accounts are removed from local storage; this design
  preserves the existing security policy.
- Changing downstream app dialogs, navigation, or session-event subscriptions.
- Changing the wire value returned by DHIS2 or the existing
  `USER_ACCOUNT_DISABLED` failure exposed to login callers.
- Refactoring all login statuses into a new public model.
- Changing upstream's direct Basic Auth login flow.

## Decisions

### Map the wire status to the existing SDK error before storing credentials

Add `D2ErrorCode.ACCOUNT_DISABLED` alongside the existing 2FA wire-status values
and extend the response-status validation immediately after `/api/auth/login` so
its string representation creates a `D2Error` with
`D2ErrorCode.USER_ACCOUNT_DISABLED`. Rename the helper from its 2FA-specific
name to reflect that it validates all terminal login statuses.

This lets the existing `LogInCall` catch path perform the established cleanup
and return the same public error consumers already understand. Because the
exception is raised before `credentialsSecureStore.set(credentials)`, the
attempt never becomes a temporary authenticated session and `/api/me` is never
called.

Alternative considered: wait for `/api/me` to return 401 and improve only the
global catcher. Rejected because it retains an unnecessary request, delays the
correct result, and ignores the authoritative status already returned by the
login endpoint.

`ACCOUNT_DISABLED` represents the protocol status only; the thrown error remains
`USER_ACCOUNT_DISABLED`, which already expresses the SDK-domain condition and is
mapped by existing consumers.

### Keep cleanup in `handleOnlineException`

The response-status validator only classifies and throws; it does not delete or
emit. `handleOnlineException()` remains the single owner of cleanup for a
disabled result produced by this login attempt. This avoids combining response
parsing with persistence side effects and prevents the global HTTP catcher plus
the login error path from both initiating deletion.

Alternative considered: invoke `UserAccountDisabledErrorCatcher` directly for
the HTTP 200 response. Rejected because that catcher is designed for failed HTTP
responses and would couple a successful transport response to HTTP-error
handling solely to reuse a side effect.

### Use focused unit tests rather than an instrumented database test

`LogInCallUnitShould` can observe the complete contract at the orchestration
boundary: returned error code, absence of `getUser(true)`, absence of credential
and authenticated-user writes, and a single cleanup request. No new database
query or deletion implementation is introduced, so a Room integration test
would duplicate coverage of the existing account-removal mechanism.

## Risks / Trade-offs

- **[Risk]** A future DHIS2 version changes the disabled wire status. → Keep the
  mapping explicit and backed by the captured `ACCOUNT_DISABLED` regression
  fixture; add new aliases only when supported by an observed server contract.
- **[Risk]** Cleanup throws because no matching local account exists. → Preserve
  the current `handleOnlineException()` behavior that catches cleanup failure
  and returns the original `USER_ACCOUNT_DISABLED` error.
- **[Risk]** Renaming the validation helper creates merge conflicts with future
  EyeSeeTea 2FA updates. → Keep the refactor local to `LogInCall` and its unit
  tests; no public API changes.
- **[Trade-off]** Downstream apps can still choose confusing navigation when
  handling account-deletion events. This SDK change guarantees the correct and
  timely error result, while app-specific presentation remains out of scope.

## Migration Plan

1. Implement and test the change on the branch based on
   `1.14.2-eyeseetea-fork-2`.
2. Publish the next EyeSeeTea patch artifact from that line.
3. Upgrade downstream apps to the new artifact and verify the disabled-account
   flow against PROD-INDIV/UAT: one `/api/auth/login` request, no `/api/me`
   request, and a visible disabled-account result.
4. Roll back by restoring the previous SDK artifact version; there are no schema
   or stored-data migrations.
