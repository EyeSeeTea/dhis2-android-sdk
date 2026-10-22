# Semicolon Separated IN Filter Values

## Purpose
Apps that build tracked entity search filters from a single text value (for
example, a list of identifiers typed or scanned as `a;b;c`) need an IN filter
from that string. The stock SDK only splits IN values that it serialized
itself with an internal separator. This customization also accepts `;` as a
separator for IN filter values, in local and online tracked entity searches.

## Requirements

### Requirement: Semicolon separates IN filter values
An IN filter value containing `;` SHALL be treated as the list of values
separated by `;`.

#### Scenario: IN filter given as a semicolon list
- **WHEN** a tracked entity search uses an IN filter with the value `a;b;c`
- **THEN** the search matches the values `a`, `b` and `c`, both locally and online

#### Scenario: IN filter built from a list
- **WHEN** a tracked entity search uses an IN filter built from a list of values that contain no `;`
- **THEN** the values are matched exactly as in the stock SDK

#### Scenario: A single value that contains a semicolon
- **WHEN** an IN filter value is one value that itself contains `;`
- **THEN** it is split at every `;` into several values
