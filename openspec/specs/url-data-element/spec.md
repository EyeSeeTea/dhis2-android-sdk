# URL Data Element Field

## Purpose
DHIS2 data elements can carry a `url` pointing to reference material
(guidelines, SOPs). The stock SDK does not download that field, so the app
cannot show it. This customization downloads, stores and exposes the data
element URL. It is the SDK side of the app's "URL data element field"
customization (consumer: WIDP).

## Requirements

### Requirement: Data element URL is downloaded and stored
The SDK SHALL request the `url` field when downloading data element metadata
and SHALL persist it with the data element.

#### Scenario: Data element with URL on the server
- **WHEN** metadata is synced and a data element has a `url` on the server
- **THEN** the stored data element has the same `url`

#### Scenario: Data element without URL
- **WHEN** metadata is synced and a data element has no `url`
- **THEN** the stored data element has no `url`

### Requirement: Data element URL is exposed to the app
The data element model returned by the SDK SHALL expose its `url`.

#### Scenario: App reads a data element
- **WHEN** the app reads a stored data element that has a `url`
- **THEN** the returned data element exposes that `url`

### Requirement: The URL column survives database migrations
Every database created by a fresh install or reached through the migration
chain SHALL have the data element URL column, without losing stored values
when a migration rebuilds the data element table.

#### Scenario: Fresh install
- **WHEN** the SDK creates a new database
- **THEN** the data element table has the URL column

#### Scenario: Migration that rebuilds the data element table
- **WHEN** a migration recreates the data element table
- **THEN** the URL column exists afterwards and keeps its values
