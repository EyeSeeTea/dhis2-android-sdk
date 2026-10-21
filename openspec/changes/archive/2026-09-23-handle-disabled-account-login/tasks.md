## 1. Handle the disabled status at the login response boundary

- [x] 1.1 Add behavior-level tests to `LogInCallUnitShould` proving that an
      `/api/auth/login` response with `loginStatus = ACCOUNT_DISABLED` returns
      `D2ErrorCode.USER_ACCOUNT_DISABLED`, never calls `getUser(true)`, does not
      persist credentials or authenticated-user state; verify the new tests fail
      against the current implementation.
- [x] 1.2 Extend the login-response status validation to map
      `ACCOUNT_DISABLED` to `USER_ACCOUNT_DISABLED` before credential persistence
      and user retrieval, and rename the 2FA-specific validation helper to match
      its broader responsibility; verify all tests from 1.1 pass.

**Commit: 1.1 + 1.2 together** as one test-first red-to-green commit.

## 2. Unify the public error contract

- [x] 2.1 Add a private/internal representation of the `ACCOUNT_DISABLED` wire
      login status and use it to produce the existing
      `D2ErrorCode.USER_ACCOUNT_DISABLED` error.
- [x] 2.2 Remove the temporary public `D2ErrorCode.ACCOUNT_DISABLED` value and
      update `core/api/core.api`; verify no production or test consumer needs a
      second disabled-account error mapping.

**Commit: 2.1 + 2.2 together** as one API-cleanup commit. This is part of the
unreleased fix and must land before publishing the artifact.

## 3. Preserve local account data during a rejected login

- [x] 3.1 Add a test proving a disabled login returns
      `USER_ACCOUNT_DISABLED` without invoking `AccountManagerImpl`.
- [x] 3.2 Remove login-time account cleanup and event emission, regardless of
      whether matching local account data exists.
- [x] 3.3 Keep `UserAccountDisabledErrorCatcher` and the upstream cleanup policy
      for established sessions unchanged.
- [x] 3.4 Run the complete `LogInCallUnitShould` test class to verify successful
      login plus TOTP, email, SMS, offline and legacy-login behavior remains
      green.

**Commit: 3.1–3.3 together** as one red-to-green session-semantics commit. Task
3.4 is verification only and does not require a commit.

## 4. Quality and release verification

- [x] 4.1 Run the SDK formatting/static checks applicable to `core` and verify
      they pass without suppressing new findings.
- [x] 4.2 Run the full `core` unit-test suite and verify there are no regressions
      outside the focused login tests.
- [x] 4.3 Against the PROD-INDIV/UAT login endpoint, verify one
      `/api/auth/login` response containing `ACCOUNT_DISABLED` produces
      `USER_ACCOUNT_DISABLED`, sends no subsequent `/api/me` request, preserves
      any local account data, and emits no account-deletion event; record the
      manual result in the implementation handoff.

**Commit: none** for Group 4 unless verification reveals a defect; any resulting
fix must be paired with its regression test in a separate red-to-green commit.

## 5. Record the surviving EyeSeeTea customization

- [x] 5.1 After implementation and verification are complete, document the
      disabled-account login handling in `eyeseetea-docs/customizations.md` and
      add the matching `// EyeSeeTea customization - Disabled account login
      handling` comment next to the minimal production logic that must survive
      upstream upgrades; verify the documented files and behavior match the
      final diff.

**Commit: 5.1 alone** as the final documentation commit, after the implementation
and verification commits are complete.
