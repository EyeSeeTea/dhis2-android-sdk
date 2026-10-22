# Tracked Entity Online Search By UIDs

## Purpose
The app sometimes needs to search the server for a known set of tracked entity
instances (for example, candidates returned by an external identification
system) while applying the usual search filters. The stock SDK applies a UID
restriction to local searches only, so the online search ignores it and
returns unrelated results. This customization sends the UID restriction to the
server as well.

## Requirements

### Requirement: Online search honours the UID restriction
When a tracked entity search is restricted to a list of UIDs, the online part
of the search SHALL send that list to the server.

#### Scenario: Online search restricted to UIDs
- **WHEN** the app runs an online tracked entity search restricted to a list of UIDs
- **THEN** the server query contains that list of UIDs
- **AND** only tracked entities with those UIDs are returned from the server

#### Scenario: Online search without a UID restriction
- **WHEN** the app runs an online tracked entity search without a UID restriction
- **THEN** the server query has no UID restriction, as in the stock SDK
