# synced-data-retention-scope Specification

## Purpose

Lets an administrator configure the retention count limit per program,
data set, and organisation unit — instead of a single fixed number for the
whole device — using the same settings model already used to configure
download limits.

## Requirements

### Requirement: The retention limit is resolved from program and data set settings
The retention count limit for tracked entity instances and events SHALL be
resolved from the program's configured trimming limit and scope, and the
retention count limit for aggregate data values SHALL be resolved from the
data set's configured trimming limit, rather than a single fixed number
applied to every program or data set.

#### Scenario: A program-specific limit overrides the global limit
- **WHEN** a program has its own configured retention count limit
- **THEN** that program's tracked entity instances and events are trimmed
  against its own limit, not the global one

#### Scenario: No program-specific limit falls back to the global limit
- **WHEN** a program has no configured retention count limit of its own
- **THEN** its tracked entity instances and events are trimmed against the
  globally configured retention count limit

#### Scenario: No configured limit at all falls back to the default
- **WHEN** neither a program-specific nor a global retention count limit is
  configured
- **THEN** the existing default retention count limit applies, unchanged
  from today's behavior

#### Scenario: A data-set-specific limit overrides the global limit
- **WHEN** a data set has its own configured retention count limit for
  aggregate data values
- **THEN** that data set's data values are trimmed against its own limit,
  not the global one

### Requirement: A per-program or per-organisation-unit scope trims each group independently
When the resolved scope for a program's retention limit splits eligible
records by program, by organisation unit, or by both, each resulting group
SHALL be trimmed against its own resolved limit, independently of every
other group's eligible record count.

#### Scenario: One program's excess does not consume another program's quota
- **WHEN** two programs each have their own configured retention count
  limit, and one program has more eligible records beyond its limit than
  the other
- **THEN** each program's excess eligible records are purged only up to its
  own limit, and the other program's eligible records within its own limit
  are left untouched

#### Scenario: A per-organisation-unit scope trims each organisation unit independently
- **WHEN** a program's retention scope is configured to split by
  organisation unit
- **THEN** eligible records are grouped by organisation unit and each group
  is trimmed against the resolved limit independently of every other
  organisation unit's eligible record count

#### Scenario: A tracked entity instance enrolled in multiple programs uses the most restrictive limit
- **WHEN** a tracked entity instance has enrollments in more than one
  program, and those programs have different configured retention count
  limits
- **THEN** the tracked entity instance is evaluated against the smallest of
  those programs' resolved limits

### Requirement: A retention limit without a matching scope field is applied globally
A retention count limit for which no per-program, per-data-set, or
per-organisation-unit scope exists SHALL be applied as a single limit across
all eligible records of that kind, unchanged from today's behavior.

#### Scenario: File resource orphan trimming has no per-program scope
- **WHEN** orphaned file resources are trimmed under their retention count
  limit
- **THEN** the limit applies to all eligible orphaned file resources as a
  single pool, with no per-program or per-organisation-unit grouping
