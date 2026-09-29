## Purpose

Purges relationships between already-synced records as part of the existing
retention purge, so that removing a tracked entity instance, enrollment, or
event never leaves behind a relationship record pointing at data that no longer
exists locally.

## ADDED Requirements

### Requirement: A relationship is purged together with its two fully synced endpoints
A relationship SHALL be purged whenever both of its linked records — regardless
of whether each is a tracked entity instance, an enrollment, or an event — are
purged by the existing retention purge. A relationship SHALL NOT be purged while
either of its two linked records still exists locally as non-eligible for purge.

#### Scenario: Purging a tracked entity instance purges its relationships
- **WHEN** an eligible tracked entity instance that has a relationship to another
  eligible tracked entity instance is purged
- **THEN** the relationship linking them is also purged

#### Scenario: A relationship between different record types is purged the same way
- **WHEN** an eligible event that has a relationship to an eligible enrollment is
  purged
- **THEN** the relationship linking them is also purged, the same way as for two
  tracked entity instances

#### Scenario: Relationship purge does not depend on relationship directionality
- **WHEN** a relationship between two eligible records is either bidirectional or
  one-directional
- **THEN** the relationship is purged the same way regardless of its
  directionality

### Requirement: A relationship with a missing counterpart record does not block purge
If one side of a relationship no longer exists locally at all — for example
because it was removed by a process other than this retention purge — the
relationship SHALL NOT indefinitely protect the remaining side from being
purged, and SHALL itself be purged once the remaining side is purged.

#### Scenario: A relationship whose counterpart record is already missing is purged
- **WHEN** an otherwise-eligible tracked entity instance has a relationship
  whose counterpart record no longer exists locally
- **THEN** the tracked entity instance is purged and the relationship is purged
  with it
