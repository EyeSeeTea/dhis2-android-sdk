## 1. Handle the disabled status at the login response boundary

- [ ] 1.1 Add behavior-level tests to `LogInCallUnitShould` proving that an
      `/api/auth/login` response with `loginStatus = ACCOUNT_DISABLED` returns
      `D2ErrorCode.USER_ACCOUNT_DISABLED`, never calls `getUser(true)`, does not
      persist credentials or authenticated-user state, and requests disabled
      account cleanup exactly once; verify the new tests fail against the current
      implementation.
- [ ] 1.2 Extend the login-response status validation to map
      `ACCOUNT_DISABLED` to `USER_ACCOUNT_DISABLED` before credential persistence
      and user retrieval, and rename the 2FA-specific validation helper to match
      its broader responsibility; verify all tests from 1.1 pass.

**Commit: 1.1 + 1.2 together** as one test-first red-to-green commit.

## 2. Preserve cleanup failure and existing login behavior

- [ ] 2.1 Add a behavior-level test proving that a cleanup failure when no local
      account exists does not replace `USER_ACCOUNT_DISABLED`; add the minimal
      implementation adjustment only if the existing catch path does not already
      satisfy the test, and verify the focused test passes.
- [ ] 2.2 Run the complete `LogInCallUnitShould` test class to verify successful
      login plus TOTP, email, SMS, offline, and legacy-login behavior remains
      green.

**Commit: 2.1 alone** as a test-only commit if current behavior already passes,
or as one red-to-green commit with its minimal fix. Task 2.2 is verification only
and does not require a commit.

## 3. Quality and release verification

- [ ] 3.1 Run the SDK formatting/static checks applicable to `core` and verify
      they pass without suppressing new findings.
- [ ] 3.2 Run the full `core` unit-test suite and verify there are no regressions
      outside the focused login tests.
- [ ] 3.3 Against the PROD-INDIV/UAT login endpoint, verify one
      `/api/auth/login` response containing `ACCOUNT_DISABLED` produces
      `USER_ACCOUNT_DISABLED`, sends no subsequent `/api/me` request, and emits
      no duplicate account-deletion event; record the manual result in the
      implementation handoff.

**Commit: none** for Group 3 unless verification reveals a defect; any resulting
fix must be paired with its regression test in a separate red-to-green commit.
