# Two-Factor Authentication Login

## Purpose
DHIS2 2.41+ servers can require a second authentication factor (TOTP, email or
SMS code) at login. The stock SDK logs in with basic authentication only, so a
user with two-factor authentication enabled cannot log in through it. This
customization lets the consuming app send a two-factor code at login and
receive a distinct error for every two-factor outcome, so it can ask for the
code, report that one was sent, or report that it was wrong.

It lives in Oslo's login flow because that flow is the only entry point for
username/password login; there is no extension point to add a new login
request from outside it.

## Requirements

### Requirement: Login accepts an optional two-factor code
The username/password login operations of the public user module SHALL accept
an optional two-factor code in addition to username, password and server URL.

#### Scenario: Login without a two-factor code
- **WHEN** the app logs in with username, password, server URL and no two-factor code
- **THEN** the SDK sends the login request without a two-factor code

#### Scenario: Login with a two-factor code
- **WHEN** the app logs in with a two-factor code
- **THEN** the SDK sends that code together with the username and password in the login request

### Requirement: Login uses the server login endpoint
The SDK SHALL authenticate username/password logins against the server's
`/api/auth/login` endpoint, and SHALL request the authenticated user's details
only after that endpoint accepts the login.

#### Scenario: Server accepts the login
- **WHEN** the login endpoint accepts the credentials
- **THEN** the SDK stores the credentials, downloads the authenticated user and completes the login

#### Scenario: Previous session cookie is not reused
- **WHEN** the SDK sends a request to the login endpoint
- **THEN** the request is sent without the stored session cookie
- **AND** the session cookie returned by the server is stored

### Requirement: Two-factor login statuses are reported as distinct errors
When the login endpoint answers with a two-factor status instead of accepting
the login, the SDK SHALL fail the login with a dedicated error code for that
status and MUST NOT complete the login.

#### Scenario: Two-factor code incorrect (2.41)
- **WHEN** the login endpoint reports the 2.41 incorrect two-factor code status
- **THEN** the login fails with `INCORRECT_TWO_FACTOR_CODE`

#### Scenario: TOTP code incorrect (2.42+)
- **WHEN** the login endpoint reports an incorrect TOTP code
- **THEN** the login fails with `INCORRECT_TWO_FACTOR_CODE_TOTP`

#### Scenario: Email code sent or incorrect (2.42+)
- **WHEN** the login endpoint reports that an email code was sent
- **THEN** the login fails with `EMAIL_TWO_FACTOR_CODE_SENT`
- **WHEN** the login endpoint reports an incorrect email code
- **THEN** the login fails with `INCORRECT_TWO_FACTOR_CODE_EMAIL`

#### Scenario: SMS code sent or incorrect (2.42+)
- **WHEN** the login endpoint reports that an SMS code was sent
- **THEN** the login fails with `SMS_TWO_FACTOR_CODE_SENT`
- **WHEN** the login endpoint reports an incorrect SMS code
- **THEN** the login fails with `INCORRECT_TWO_FACTOR_CODE_SMS`

#### Scenario: Too many code send attempts
- **WHEN** the login endpoint reports too many two-factor code send attempts
- **THEN** the login fails with `TWO_FACTOR_MANY_SEND_ATTEMPTS`

#### Scenario: Failed login leaves no session
- **WHEN** the login fails with any two-factor error
- **THEN** no credentials or authenticated user are retained for that attempt

### Requirement: Servers without the login endpoint keep working
The SDK SHALL fall back to the legacy basic-authentication login when the
server does not provide the login endpoint, so servers older than 2.41 can
still be used.

#### Scenario: Server without the login endpoint
- **WHEN** the login endpoint request fails because the server is not reachable as a DHIS2 login endpoint or answers unexpectedly
- **THEN** the SDK retries the login with basic authentication against the same server
- **AND** a successful basic-authentication login completes the login as before

#### Scenario: Legacy login also fails
- **WHEN** the basic-authentication fallback fails
- **THEN** the login fails with the fallback's error
- **AND** no credentials or user identifier are retained

### Requirement: Offline login is unchanged
A login that fails because the device is offline SHALL still fall back to the
existing offline login, regardless of the two-factor code.

#### Scenario: Offline login with a known account
- **WHEN** the login cannot reach the server and the account already exists on the device with the same credentials
- **THEN** the login succeeds offline as in the stock SDK
