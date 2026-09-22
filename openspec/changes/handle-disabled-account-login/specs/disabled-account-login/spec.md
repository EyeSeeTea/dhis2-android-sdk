## Purpose

Define deterministic SDK behavior when a DHIS2 login response reports that the
user account is disabled, so consumers receive the correct failure without a
misleading follow-up authentication request.

## ADDED Requirements

### Requirement: Disabled login status is terminal
The SDK SHALL treat `ACCOUNT_DISABLED` returned by the username/password login
endpoint as a terminal authentication failure and SHALL report the existing
disabled-account error to the caller.

#### Scenario: Login endpoint reports a disabled account
- **WHEN** the login endpoint returns HTTP 200 with `loginStatus` equal to `ACCOUNT_DISABLED`
- **THEN** the login operation fails with `USER_ACCOUNT_DISABLED`
- **AND** the SDK does not request the authenticated user's details
- **AND** the SDK does not report the result as bad credentials

### Requirement: Disabled login does not establish a session
The SDK MUST NOT persist the submitted credentials or authenticated-user state
when the login endpoint reports `ACCOUNT_DISABLED`.

#### Scenario: Disabled response arrives before user retrieval
- **WHEN** the login endpoint reports `ACCOUNT_DISABLED`
- **THEN** the submitted credentials are not retained as an authenticated session
- **AND** no authenticated user is stored for that attempt

### Requirement: Disabled-account cleanup occurs at most once
The SDK SHALL preserve its existing local-account removal policy for a disabled
account, but MUST initiate at most one account removal and one corresponding
disabled-account notification for a single failed login attempt.

#### Scenario: A local account exists for the disabled user
- **WHEN** a login attempt for that account receives `ACCOUNT_DISABLED`
- **THEN** the SDK removes the local account according to the existing disabled-account policy
- **AND** emits the disabled-account deletion reason once

#### Scenario: No local account exists for the disabled user
- **WHEN** a first login attempt receives `ACCOUNT_DISABLED` and no matching local account exists
- **THEN** the SDK still returns `USER_ACCOUNT_DISABLED`
- **AND** cleanup failure does not replace or hide the authentication error

### Requirement: Other login statuses retain their behavior
The SDK SHALL preserve the existing behavior for successful authentication and
all supported two-factor authentication statuses.

#### Scenario: Login succeeds
- **WHEN** the login endpoint returns `SUCCESS`
- **THEN** the SDK continues by retrieving and establishing the authenticated user

#### Scenario: Login requires or rejects a second factor
- **WHEN** the login endpoint returns a supported TOTP, email, or SMS two-factor status
- **THEN** the SDK reports the corresponding existing two-factor result
- **AND** does not retrieve the authenticated user's details
