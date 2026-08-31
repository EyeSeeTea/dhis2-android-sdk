## Purpose

Lets a caller reclaim device storage by removing already-synced local data
(tracker and aggregate records, and their attached files) once it exceeds a
configured retention count, without ever removing data that still has pending
changes anywhere in its record tree.

## ADDED Requirements

### Requirement: Purge eligibility requires a fully synced record tree
A record, or for hierarchical data its entire ancestor-descendant tree, SHALL be
eligible for purge only when its aggregated sync state is fully synced. A single
non-synced record anywhere in the tree SHALL protect every record in that tree
from purge, including the otherwise-eligible ancestors and descendants.

#### Scenario: Fully synced tree is eligible
- **WHEN** a tracked entity instance and all of its enrollments, events, and
  values are fully synced
- **THEN** the tracked entity instance is eligible for purge

#### Scenario: One pending descendant protects the whole tree
- **WHEN** a tracked entity instance is fully synced but one of its events has a
  pending change
- **THEN** neither the tracked entity instance, its enrollments, nor any of its
  events or values are purged

#### Scenario: Leaf data without children uses its own sync state
- **WHEN** an aggregate data value has no child records
- **THEN** its eligibility is determined directly by its own sync state, not an
  aggregated one

### Requirement: Purge respects a caller-supplied retention count limit
Given a retention count limit, the system SHALL purge only the excess: the
oldest eligible records, ordered by last-updated time, beyond the limit. Records
within the limit SHALL be left untouched, regardless of their sync state.

#### Scenario: Only the oldest excess records are purged
- **WHEN** there are more eligible records than the configured limit
- **THEN** the oldest eligible records beyond the limit are purged and the most
  recently updated eligible records up to the limit remain

#### Scenario: Non-eligible records are never counted toward the limit
- **WHEN** counting records against the limit
- **THEN** records that are not fully synced are excluded from the count and
  from purge, even if they are older than eligible records that were kept

#### Scenario: A limit of zero purges everything eligible
- **WHEN** the retention count limit is zero
- **THEN** every eligible record is purged, leaving no fully synced records
  behind

#### Scenario: Fewer eligible records than the limit purges nothing
- **WHEN** the number of eligible records is at or below the configured limit
- **THEN** no records are purged

### Requirement: Hierarchical data is purged as a complete tree
When an eligible tracked entity instance is purged, every record in its tree —
its enrollments, their events, and all attached values — SHALL be purged
together as a single unit, never leaving an orphaned child or a childless
partial purge.

#### Scenario: Purging a tracked entity instance removes its full tree
- **WHEN** an eligible tracked entity instance is purged
- **THEN** all of its enrollments, their events, and all attached values are
  also removed

### Requirement: Leaf data without a tree is trimmed independently
Aggregate data values and events with no tracked entity instance SHALL be
subject to retention trimming independently of tracked-entity-rooted data, each
under its own retention count limit.

#### Scenario: Trimming leaf data does not affect tracked entity data
- **WHEN** aggregate data values are trimmed under their retention limit
- **THEN** tracked entity instances, enrollments, and their events are
  unaffected by that operation

### Requirement: Purge is transactional
A purge operation SHALL either complete fully or leave the local data
completely unchanged. A failure partway through the operation SHALL NOT result
in a partial purge.

#### Scenario: A failure partway through leaves data unchanged
- **WHEN** an unexpected failure occurs after some but not all eligible records
  have been removed within a single purge operation
- **THEN** the local data reflects the same state as before the purge operation
  started, with no records removed

### Requirement: Purging a file resource removes its physical file
Purging an eligible file resource SHALL remove both its local record and its
associated physical file. The absence of the physical file on the device SHALL
NOT prevent the record from being purged or raise an error to the caller.

#### Scenario: Purging a file resource with an existing physical file
- **WHEN** an eligible file resource is purged and its physical file exists on
  the device
- **THEN** both the record and the physical file are removed

#### Scenario: Purging a file resource whose physical file is already missing
- **WHEN** an eligible file resource is purged but its physical file no longer
  exists on the device
- **THEN** the record is still purged and no error is raised to the caller
