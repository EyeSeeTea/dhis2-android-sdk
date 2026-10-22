# Automatic Logout On Unauthorized Session

## Purpose
When the account's server answers a username/password request with HTTP 401,
the stock SDK only returns the error to that request, and the app has no
signal that the session is no longer valid. This customization logs the user
out of the SDK and sends an event to the app through
`AccountManager.logOutObservable()`, so the app can take the user back to the
login screen.

The event is emitted from the SDK's authentication layer because that is the
only place that sees every authenticated response.

## Requirements

### Requirement: Unauthorized response logs the user out
When an authenticated username/password request to the account's server is
answered with HTTP 401, the SDK SHALL remove the stored credentials and SHALL
notify the logout to the app.

#### Scenario: Session rejected by the account's server
- **WHEN** a request to the logged-in account's server returns HTTP 401
- **THEN** the stored credentials are removed
- **AND** the account manager's logout observable emits

#### Scenario: Request still returns its response
- **WHEN** the SDK logs the user out because of a 401 response
- **THEN** the request that received it still returns that response to its caller

### Requirement: Only the account's own session triggers the logout
The SDK MUST NOT log the user out for unauthorized answers that do not mean
the account's session was rejected.

#### Scenario: Unauthorized answer to a login attempt
- **WHEN** a request to the login endpoint returns HTTP 401
- **THEN** no logout happens and the login fails as usual

#### Scenario: Unauthorized answer from another host
- **WHEN** a request to a host other than the account's server returns HTTP 401
- **THEN** no logout happens

### Requirement: The app can observe logouts
The account manager SHALL expose an observable that emits every time the SDK
logs the user out this way.

#### Scenario: App subscribed to logouts
- **WHEN** the app subscribes to the logout observable and the SDK logs the user out because of a 401 response
- **THEN** the subscriber receives one emission
