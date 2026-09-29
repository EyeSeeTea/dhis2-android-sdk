## MODIFIED Requirements

### Requirement: Purge respects a caller-supplied retention count limit
Given a retention count limit, the system SHALL purge only the excess: the
oldest eligible records, ordered by last-updated time, beyond the limit.
Records within the limit SHALL be left untouched, regardless of their sync
state. The retention count limit itself, and whether it applies to all
eligible records together or to independent groups of them, is resolved as
described by the `synced-data-retention-scope` capability; this requirement
governs how the limit is applied once resolved, not how it is obtained.

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
