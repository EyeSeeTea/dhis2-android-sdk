# Change Server URL

## Purpose
Some clients move their DHIS2 instance to a new address (domain change,
migration behind a new proxy). In the stock SDK, an account is tied to the
server URL it was created with, so after such a move the user would have to log
in to a new, empty account and lose unsynced data. This customization lets the
app point the logged-in account at a new server URL while keeping its local
database.

## Requirements

### Requirement: The logged-in account can switch to a new server URL
The account manager SHALL let the app change the server URL of the logged-in
account. After the change, the SDK SHALL send requests to the new URL and SHALL
keep using the same local database.

#### Scenario: Server URL changed while logged in
- **WHEN** the app changes the server URL of the logged-in account
- **THEN** subsequent requests go to the new server URL
- **AND** the stored credentials carry the new server URL
- **AND** the account in the databases configuration carries the new server URL with its existing database

#### Scenario: Other accounts are not affected
- **WHEN** the server URL of the logged-in account is changed
- **THEN** accounts for other users or other servers keep their server URL

### Requirement: Logging in again reuses the existing account
When a user logs in to a server URL and username that already have an account
on the device, the SDK SHALL reuse that account and its database instead of
creating a new one, provided its encryption setting matches.

#### Scenario: Login after a server URL change
- **WHEN** a user whose account was moved to a new server URL logs in again with that URL
- **THEN** the SDK opens the account's existing database

#### Scenario: URLs that differ only in normalization
- **WHEN** a user logs in with a server URL that differs from the stored one only in its normalized form
- **THEN** the SDK reuses the existing account

#### Scenario: Different encryption setting
- **WHEN** a user logs in to an existing server URL and username but with a different encryption setting
- **THEN** the SDK creates a new account instead of reusing the existing one
